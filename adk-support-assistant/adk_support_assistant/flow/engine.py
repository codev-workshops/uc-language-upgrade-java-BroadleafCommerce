"""Deterministic flow engine (finite state machine).

Responsibilities:
  * render entry / trigger fulfillment messages (via i18n bundles, falling back
    to the flow's inline copy),
  * slot filling with initial prompt and reprompts,
  * safe evaluation of transition conditions over session/page state,
  * webhook invocation (``fetchOrderStatus``) on page entry,
  * page-scoped AND flow-scoped (global) transition routes and event handlers,
  * automatic cascading through condition-only routes within a single turn,
  * no-match escalation culminating in live-agent handoff,
  * page/flow transitions, tolerating targets that are not defined in the flow
    (treated as terminal).

The engine is intentionally free of any network/LLM dependency: NLU is injected
as an :class:`NluClient`, and order lookup as an :class:`OrderLookupClient`.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any

from ..i18n.bundle import ResourceBundle, detect_locale, render_template
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
LOOKUP_FAILED_ID = "system.error.lookup_failed"
_MAX_STEPS = 12


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
        return self.bundles.get(self.default_locale) or next(iter(self.bundles.values()))

    def _render_message(self, message, state: SessionState) -> str | None:
        bundle = self._bundle(state)
        if bundle.has(message.id):
            return bundle.render(message.id, state.session_params)
        if message.default_text:
            return render_template(message.default_text, state.session_params)
        return None

    def _render(self, fulfillment: Fulfillment, state: SessionState) -> list[str]:
        out: list[str] = []
        for message in fulfillment.messages:
            text = self._render_message(message, state)
            if text is not None:
                out.append(text)
        return out

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
            if param.required and state.session_params.get(param.display_name) is None:
                return param
        return None

    def _enter_page(
        self, page_name: str, state: SessionState, trace: TurnTrace
    ) -> tuple[list[str], bool]:
        """Enter ``page_name``. Returns (messages, webhook_failed).

        A target that is not defined in the flow (e.g. ``MainMenu`` /
        ``FollowUpFlow`` in a partial export) is treated as terminal: the current
        page is updated but no entry/form processing occurs.
        """
        state.current_page = page_name
        trace.to_page = page_name
        page = self.flow.page_or_none(page_name)
        if page is None:
            return [], False

        state.reset_page_form()
        state.no_match_count = 0
        messages = self._apply_fulfillment(page.entry_fulfillment, state, trace)

        failed = False
        if self._fulfillment_calls_lookup(page.entry_fulfillment):
            err, failed = self._run_order_lookup(state, trace)
            messages += err

        if not failed:
            param = self._first_unfilled_required(page, state)
            if param is not None:
                messages += self._apply_fulfillment(
                    param.fill_behavior.initial_prompt_fulfillment, state, trace
                )
        return messages, failed

    @staticmethod
    def _fulfillment_calls_lookup(fulfillment: Fulfillment) -> bool:
        return fulfillment.tag == FETCH_ORDER_STATUS_TAG or bool(fulfillment.webhook)

    def _run_order_lookup(
        self, state: SessionState, trace: TurnTrace
    ) -> tuple[list[str], bool]:
        """Run the order-status webhook, populating session params.

        Returns (error_messages, failed). On failure the session is escalated to
        a live agent.
        """
        order_id = state.session_params.get("order_id")
        if self.order_client is None:
            return self._lookup_failure(state, trace, order_id)
        started = time.perf_counter()
        try:
            result = self.order_client.lookup(str(order_id))
        except Exception:  # noqa: BLE001 - any client error -> graceful fallback
            trace.record_tool(FETCH_ORDER_STATUS_TAG, {"order_id": order_id}, 0.0, ok=False)
            return self._lookup_failure(state, trace, order_id)
        duration = (time.perf_counter() - started) * 1000
        ok = result.error is None
        trace.record_tool(FETCH_ORDER_STATUS_TAG, {"order_id": order_id}, duration, ok=ok)
        if not ok:
            return self._lookup_failure(state, trace, order_id)
        state.session_params["order_status"] = result.status
        state.session_params["order_found"] = result.found
        if result.carrier is not None:
            state.session_params["carrier"] = result.carrier
        return [], False

    def _lookup_failure(
        self, state: SessionState, trace: TurnTrace, order_id: Any
    ) -> tuple[list[str], bool]:
        state.handoff = True
        state.handoff_reason = "webhook_failure"
        state.handoff_metadata = {"reason": "webhook_failure", "order_id": order_id}
        trace.handoff = True
        trace.events.append("webhook_failure")
        bundle = self._bundle(state)
        msgs: list[str] = []
        if bundle.has(LOOKUP_FAILED_ID):
            msgs.append(bundle.render(LOOKUP_FAILED_ID, state.session_params))
            trace.emitted_message_ids.append(LOOKUP_FAILED_ID)
        return msgs, True

    # -- public API --------------------------------------------------------

    def start(self, state: SessionState) -> TurnResult:
        trace = get_tracer(self.redact_pii).new_turn(state.session_id)
        if state.locale is None:
            state.locale = self.default_locale
        trace.locale = state.locale
        messages, _ = self._enter_page(self.flow.start_page, state, trace)
        state.started = True
        result = self._result(messages, state, trace, intent=None, entities={})
        trace.emit()
        return result

    def handle(self, state: SessionState, user_text: str) -> TurnResult:
        trace = get_tracer(self.redact_pii).new_turn(state.session_id)
        trace.start("turn")
        trace.input_text = user_text
        trace.from_page = state.current_page

        if not state.started or state.current_page is None:
            self.start(state)

        if state.locale is None:
            state.locale = detect_locale(
                user_text, self.supported_locales, self.default_locale
            )
        trace.locale = state.locale

        trace.start("nlu")
        nlu = self.nlu.classify(user_text, state.locale)
        trace.stop("nlu")
        trace.intent = nlu.intent
        trace.confidence = nlu.confidence
        trace.entities = dict(nlu.entities)

        page = self.flow.page_or_none(state.current_page) or Page(
            display_name=state.current_page or ""
        )

        pending = self._first_unfilled_required(page, state)
        page_intent_matches = self._matching_intent_route(page, nlu.intent) is not None

        if pending is not None and not page_intent_matches:
            result = self._do_slot_filling(page, pending, state, nlu, trace)
        else:
            result = self._route_and_advance(page, state, nlu, trace, allow_intent=True)

        trace.stop("turn")
        trace.emit()
        return result

    # -- slot filling ------------------------------------------------------

    def _do_slot_filling(
        self, page: Page, param, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        value = nlu.entities.get(param.display_name)
        if value is None and param.entity_type == "sys.number":
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
                msgs = (
                    self._apply_fulfillment(handler.trigger_fulfillment, state, trace)
                    if handler
                    else []
                )
                return self._result(msgs, state, trace, nlu.intent, nlu.entities)

        state.session_params[param.display_name] = value
        state.no_match_count = 0
        trace.chosen_route = f"fill:{param.display_name}"

        next_param = self._first_unfilled_required(page, state)
        if next_param is not None:
            msgs = self._apply_fulfillment(
                next_param.fill_behavior.initial_prompt_fulfillment, state, trace
            )
            return self._result(msgs, state, trace, nlu.intent, nlu.entities)

        # Form complete: evaluate the page's transition routes (and cascade).
        return self._route_and_advance(page, state, nlu, trace, allow_intent=True)

    def _slot_no_match(
        self, page: Page, param, state: SessionState, nlu: NluResult, trace: TurnTrace
    ) -> TurnResult:
        state.no_match_count += 1
        if state.no_match_count >= MAX_NO_MATCH:
            event = NO_MATCH_2
            trace.events.append(event)
            trace.chosen_route = f"reprompt:{event}"
            handler = self._event_handler(page, event)
            msgs = (
                self._apply_fulfillment(handler.trigger_fulfillment, state, trace)
                if handler
                else []
            )
            return self._result(msgs, state, trace, nlu.intent, nlu.entities)

        event = NO_MATCH_1
        trace.events.append(event)
        trace.chosen_route = f"reprompt:{event}"
        fulfillment = self._reprompt_fulfillment(page, param, state.no_match_count)
        msgs = self._apply_fulfillment(fulfillment, state, trace) if fulfillment else []
        return self._result(msgs, state, trace, nlu.intent, nlu.entities)

    def _reprompt_fulfillment(self, page: Page, param, attempt: int) -> Fulfillment | None:
        reprompts = param.fill_behavior.reprompt_fulfillments
        if reprompts:
            return reprompts[min(attempt - 1, len(reprompts) - 1)]
        handler = page.reprompt_handler(param.display_name, NO_MATCH_1)
        if handler is not None:
            return handler.trigger_fulfillment
        handler = self._event_handler(page, NO_MATCH_1)
        return handler.trigger_fulfillment if handler else None

    # -- routing -----------------------------------------------------------

    def _routes(self, page: Page) -> list[TransitionRoute]:
        """Page-scoped routes first, then flow-scoped (global) routes."""
        return list(page.transition_routes) + list(self.flow.transition_routes)

    def _matching_intent_route(self, page: Page, intent: str | None) -> TransitionRoute | None:
        if intent is None:
            return None
        for route in self._routes(page):
            if route.intent == intent:
                return route
        return None

    def _event_handler(self, page: Page, event: str) -> EventHandler | None:
        return page.event_handler(event) or self.flow.event_handler(event)

    def _first_matching_route(
        self, page: Page, state: SessionState, intent: str | None, allow_intent: bool
    ) -> TransitionRoute | None:
        ctx = state.condition_context(intent)
        for route in self._routes(page):
            if route.intent is not None:
                if not allow_intent or route.intent != intent:
                    continue
                if route.condition is not None and not evaluate(route.condition, ctx):
                    continue
                return route
            if route.condition is not None and evaluate(route.condition, ctx):
                return route
        return None

    def _route_and_advance(
        self,
        page: Page,
        state: SessionState,
        nlu: NluResult,
        trace: TurnTrace,
        allow_intent: bool,
    ) -> TurnResult:
        msgs: list[str] = []
        cur: Page | None = page
        first = True
        for _ in range(_MAX_STEPS):
            if cur is None:
                break
            route = self._first_matching_route(
                cur, state, nlu.intent, allow_intent=(first and allow_intent)
            )
            if route is None:
                if first:
                    return self._page_no_match(cur, state, nlu, trace, msgs)
                break
            first = False
            state.no_match_count = 0
            trace.chosen_route = (
                f"route:{route.name or ''}(intent={route.intent},cond={route.condition!r})"
            )
            msgs += self._apply_fulfillment(route.trigger_fulfillment, state, trace)

            target = route.target_page or route.target_flow
            if target is None:
                break
            enter_msgs, failed = self._enter_page(target, state, trace)
            msgs += enter_msgs
            if failed:
                break
            cur = self.flow.page_or_none(target)
            if cur is None:
                break  # terminal / undefined target
            if self._first_unfilled_required(cur, state) is not None:
                break  # a new form now awaits user input
        return self._result(msgs, state, trace, nlu.intent, nlu.entities)

    def _page_no_match(
        self,
        page: Page,
        state: SessionState,
        nlu: NluResult,
        trace: TurnTrace,
        base_msgs: list[str],
    ) -> TurnResult:
        state.no_match_count += 1
        event = NO_MATCH_1 if state.no_match_count < MAX_NO_MATCH else NO_MATCH_2
        trace.events.append(event)
        trace.chosen_route = f"no-match:{event}"
        handler = self._event_handler(page, event)
        msgs = list(base_msgs)
        if handler is not None:
            msgs += self._apply_fulfillment(handler.trigger_fulfillment, state, trace)
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
