"""Deterministic flow engine (finite state machine).

Responsibilities:
  * render entry / trigger fulfillment messages (via i18n bundles),
  * slot filling with initial prompt, reprompts and validation,
  * safe evaluation of transition conditions over session/page state,
  * webhook invocation on form completion (fetchOrderStatus),
  * no-match escalation culminating in live-agent handoff,
  * page/flow transitions.

The engine is intentionally free of any network/LLM dependency: NLU is injected
as an :class:`NluClient`, and order lookup as an :class:`OrderLookupClient`.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any

from ..i18n.bundle import ResourceBundle, detect_locale
from ..nlu.classifier import NluClient
from ..nlu.schema import NluResult
from ..observability.tracing import TurnTrace, get_tracer
from ..session.state import SessionState
from ..tools.order_lookup import OrderLookupClient
from .conditions import evaluate
from .schema import EventHandler, Flow, Fulfillment, Page, TransitionRoute

NO_MATCH_1 = "sys.no-match-1"
NO_MATCH_2 = "sys.no-match-2"
INVALID_PARAM = "sys.invalid-param"
MAX_NO_MATCH = 2
FETCH_ORDER_STATUS_TAG = "fetchOrderStatus"


@dataclass
class TurnResult:
    messages: list[str] = field(default_factory=list)
    current_page: str | None = None
    handoff: bool = False
    handoff_reason: str | None = None
    intent: str | None = None
    entities: dict[str, Any] = field(default_factory=dict)
    locale: str = "en"
    message_ids: list[str] = field(default_factory=list)
    trace: TurnTrace | None = None


class FlowEngine:
    def __init__(
        self,
        flow: Flow,
        bundles: dict[str, ResourceBundle],
        nlu_client: NluClient,
        order_client: OrderLookupClient | None = None,
        default_locale: str = "en",
        supported_locales: tuple[str, ...] = ("en", "es"),
        redact_pii: bool = True,
    ):
        self.flow = flow
        self.bundles = bundles
        self.nlu = nlu_client
        self.order_client = order_client
        self.default_locale = default_locale
        self.supported_locales = supported_locales
        self.redact_pii = redact_pii

    # -- rendering ---------------------------------------------------------

    def _bundle(self, state: SessionState) -> ResourceBundle:
        locale = state.locale or self.default_locale
        if locale in self.bundles:
            return self.bundles[locale]
        return self.bundles[self.default_locale]

    def _render(self, fulfillment: Fulfillment, state: SessionState) -> list[str]:
        bundle = self._bundle(state)
        return [bundle.render(mid, state.session_params) for mid in fulfillment.message_ids]

    def _apply_fulfillment(
        self, fulfillment: Fulfillment, state: SessionState, trace: TurnTrace
    ) -> list[str]:
        """Apply side effects (set params, handoff) and render messages."""
        for action in fulfillment.set_parameter_actions:
            state.session_params[action.parameter] = action.value
        if fulfillment.live_agent_handoff is not None:
            state.handoff = True
            meta = dict(fulfillment.live_agent_handoff.metadata)
            state.handoff_reason = str(meta.get("reason", "unspecified"))
            state.handoff_metadata = meta
            trace.handoff = True
        trace.emitted_message_ids.extend(fulfillment.message_ids)
        return self._render(fulfillment, state)

    # -- page entry --------------------------------------------------------

    def _first_unfilled_required(self, page: Page, state: SessionState):
        for param in page.form.parameters:
            if param.required and param.display_name not in state.session_params:
                return param
        return None

    def _enter_page(self, page_name: str, state: SessionState, trace: TurnTrace) -> list[str]:
        page = self.flow.page(page_name)
        state.current_page = page_name
        state.reset_page_form()
        state.no_match_count = 0
        messages = self._apply_fulfillment(page.entry_fulfillment, state, trace)
        param = self._first_unfilled_required(page, state)
        if param is not None:
            messages += self._apply_fulfillment(
                param.fill_behavior.initial_prompt_fulfillment, state, trace
            )
        trace.to_page = page_name
        return messages

    # -- public API --------------------------------------------------------

    def start(self, state: SessionState) -> TurnResult:
        trace = get_tracer(self.redact_pii).new_turn(state.session_id)
        if state.locale is None:
            state.locale = self.default_locale
        messages = self._enter_page(self.flow.start_page, state, trace)
        state.started = True
        return self._result(messages, state, trace, intent=None, entities={})

    def handle(self, state: SessionState, user_text: str) -> TurnResult:
        trace = get_tracer(self.redact_pii).new_turn(state.session_id)
        trace.start("turn")
        trace.input_text = user_text
        trace.from_page = state.current_page

        if not state.started or state.current_page is None:
            # Lazily start if the caller forgot to.
            self.start(state)

        # Locale detection (only when not yet locked for the session).
        if state.locale is None:
            state.locale = detect_locale(
                user_text, self.supported_locales, self.default_locale
            )
        trace.locale = state.locale

        # NLU.
        trace.start("nlu")
        nlu = self.nlu.classify(user_text, state.locale)
        trace.stop("nlu")
        trace.intent = nlu.intent
        trace.confidence = nlu.confidence
        trace.entities = dict(nlu.entities)

        page = self.flow.page(state.current_page)

        pending = self._first_unfilled_required(page, state)
        page_intent_matches = self._matching_intent_route(page, nlu.intent) is not None

        if pending is not None and not page_intent_matches:
            result = self._do_slot_filling(page, pending, state, nlu, trace)
        else:
            result = self._do_routing(page, state, nlu, trace)

        trace.stop("turn")
        trace.emit()
        return result

    # -- slot filling ------------------------------------------------------

    def _do_slot_filling(
        self, page: Page, param, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        value = nlu.entities.get(param.display_name)
        if value is None and param.entity_type == "sys.number":
            # Fall back to any extracted numeric entity.
            value = nlu.entities.get("order_id")

        if value is None:
            return self._slot_no_match(page, param, state, nlu, trace)

        if param.validation is not None:
            ctx = {
                "session": {"params": {**state.session_params, param.display_name: value}},
                "page": {"params": state.page_params},
                "flow": {"params": state.session_params},
                "intent": nlu.intent,
            }
            if not evaluate(param.validation.condition, ctx):
                trace.events.append(INVALID_PARAM)
                trace.chosen_route = f"invalid:{param.display_name}"
                handler = page.reprompt_handler(param.display_name, param.validation.invalid_event)
                msgs = self._apply_fulfillment(handler.trigger_fulfillment, state, trace) if handler else []
                return self._result(msgs, state, trace, nlu.intent, nlu.entities)

        # Valid: fill slot.
        state.session_params[param.display_name] = value
        state.no_match_count = 0
        trace.chosen_route = f"fill:{param.display_name}"

        next_param = self._first_unfilled_required(page, state)
        if next_param is not None:
            msgs = self._apply_fulfillment(
                next_param.fill_behavior.initial_prompt_fulfillment, state, trace
            )
            return self._result(msgs, state, trace, nlu.intent, nlu.entities)

        # Form complete.
        return self._on_form_complete(page, state, nlu, trace)

    def _slot_no_match(
        self, page: Page, param, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        state.no_match_count += 1
        event = NO_MATCH_1 if state.no_match_count < MAX_NO_MATCH else NO_MATCH_2
        trace.events.append(event)
        trace.chosen_route = f"reprompt:{event}"
        handler = page.reprompt_handler(param.display_name, event)
        if handler is None:
            handler = page.event_handler(event)
        msgs = self._apply_fulfillment(handler.trigger_fulfillment, state, trace) if handler else []
        return self._result(msgs, state, trace, nlu.intent, nlu.entities)

    def _on_form_complete(
        self, page: Page, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        state.page_params["status"] = "FINAL"
        # Run webhook(s) referenced by the page's transition routes.
        error_msgs, failed = self._run_webhooks(page, state, trace)
        if failed:
            # Graceful fallback: escalate to a live agent.
            state.handoff = True
            state.handoff_reason = "webhook_failure"
            trace.handoff = True
            bundle = self._bundle(state)
            msgs = list(error_msgs)
            if bundle.has("system.error.lookup_failed"):
                msgs.append(bundle.render("system.error.lookup_failed", state.session_params))
                trace.emitted_message_ids.append("system.error.lookup_failed")
            return self._result(msgs, state, trace, nlu.intent, nlu.entities)
        return self._evaluate_and_take_route(page, state, nlu, trace)

    def _run_webhooks(
        self, page: Page, state: SessionState, trace: TurnTrace
    ) -> tuple[list[str], bool]:
        tags = {
            route.trigger_fulfillment.tag
            for route in page.transition_routes
            if route.trigger_fulfillment.tag
        }
        if FETCH_ORDER_STATUS_TAG in tags:
            if self.order_client is None:
                return [], True
            order_id = state.session_params.get("order_id")
            started = time.perf_counter()
            try:
                result = self.order_client.lookup(str(order_id))
            except Exception:  # noqa: BLE001 - any client error -> graceful fallback
                trace.record_tool(FETCH_ORDER_STATUS_TAG, {"order_id": order_id}, 0.0, ok=False)
                return [], True
            duration = (time.perf_counter() - started) * 1000
            trace.record_tool(
                FETCH_ORDER_STATUS_TAG, {"order_id": order_id}, duration, ok=result.error is None
            )
            if result.error is not None:
                return [], True
            state.session_params["order_status"] = result.status
            state.session_params["order_found"] = result.found
        return [], False

    # -- routing -----------------------------------------------------------

    def _matching_intent_route(self, page: Page, intent: str | None) -> TransitionRoute | None:
        if intent is None:
            return None
        for route in page.transition_routes:
            if route.intent == intent:
                return route
        return None

    def _do_routing(
        self, page: Page, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        return self._evaluate_and_take_route(page, state, nlu, trace)

    def _route_matches(
        self, route: TransitionRoute, state: SessionState, intent: str | None
    ) -> bool:
        if route.intent is not None and route.intent != intent:
            return False
        if route.condition is not None:
            ctx = state.condition_context(intent)
            if not evaluate(route.condition, ctx):
                return False
        # A route must have at least one gate that matched.
        return route.intent is not None or route.condition is not None

    def _evaluate_and_take_route(
        self, page: Page, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        for route in page.transition_routes:
            if self._route_matches(route, state, nlu.intent):
                state.no_match_count = 0
                trace.chosen_route = (
                    f"route:intent={route.intent},cond={route.condition!r}"
                )
                msgs = self._apply_fulfillment(route.trigger_fulfillment, state, trace)
                target = route.target_page
                if target is not None and self.flow.has_page(target):
                    msgs += self._enter_page(target, state, trace)
                return self._result(msgs, state, trace, nlu.intent, nlu.entities)
        # No route matched -> page-level no-match handling.
        return self._page_no_match(page, state, nlu, trace)

    def _page_no_match(
        self, page: Page, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        state.no_match_count += 1
        event = NO_MATCH_1 if state.no_match_count < MAX_NO_MATCH else NO_MATCH_2
        trace.events.append(event)
        trace.chosen_route = f"no-match:{event}"
        handler: EventHandler | None = page.event_handler(event)
        msgs = self._apply_fulfillment(handler.trigger_fulfillment, state, trace) if handler else []
        return self._result(msgs, state, trace, nlu.intent, nlu.entities)

    # -- result ------------------------------------------------------------

    def _result(
        self,
        messages: list[str],
        state: SessionState,
        trace: TurnTrace,
        intent: str | None,
        entities: dict[str, Any],
    ) -> TurnResult:
        return TurnResult(
            messages=messages,
            current_page=state.current_page,
            handoff=state.handoff,
            handoff_reason=state.handoff_reason,
            intent=intent,
            entities=dict(entities),
            locale=state.locale or self.default_locale,
            message_ids=list(trace.emitted_message_ids),
            trace=trace,
        )
