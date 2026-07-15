"""Parse a Dialogflow-CX-style flow export into the internal :mod:`schema`.

The loader is tolerant of the shapes Dialogflow CX uses:
  * ``camelCase`` keys (as exported by the CX API / gcloud), and
  * already-normalised ``snake_case`` keys.

Resource paths (``.../pages/WelcomePage``, ``.../intents/Intent_Cancel_Order``,
``.../entityTypes/sys.number``) are reduced to their short display name.

Messages in a raw CX export have inline text and no stable ``id``. The loader
generates a deterministic ``id`` for every message (used as the i18n key) and
keeps the inline copy as ``default_text`` so the flow works with or without
locale bundles. CX-style ``$session.params.x`` interpolation is normalised to
``{x}`` placeholders.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from .schema import (
    EventHandler,
    FillBehavior,
    Flow,
    Form,
    Fulfillment,
    LiveAgentHandoff,
    Message,
    Page,
    Parameter,
    SetParameterAction,
    TrainingIntent,
    TransitionRoute,
    Validation,
    Webhook,
)

_VAR_RE = re.compile(r"\$(?:session|page|flow)\.params\.([A-Za-z_][A-Za-z0-9_]*)")


def _get(d: dict[str, Any], *keys: str, default: Any = None) -> Any:
    """Return the first present key among ``keys`` (camelCase or snake_case)."""
    for key in keys:
        if key in d and d[key] is not None:
            return d[key]
    return default


def _leaf(value: Any) -> Any:
    """Reduce a CX resource path to its last path segment (display name)."""
    if isinstance(value, str) and "/" in value:
        return value.rsplit("/", 1)[-1]
    return value


def _normalize_text(text: str) -> str:
    """Convert CX ``$session.params.x`` references to ``{x}`` placeholders."""
    return _VAR_RE.sub(r"{\1}", text)


def _message_text(m: dict[str, Any]) -> str | None:
    text = _get(m, "text", default=None)
    if isinstance(text, dict):
        texts = text.get("text")
        if texts:
            return str(texts[0])
    return None


def _parse_messages(raw_messages: Any, prefix: str) -> list[Message]:
    """Parse a message list, generating ids as ``{prefix}.{index}``.

    Entries may be plain strings (older CX reprompt shape) or objects with a
    ``text.text`` payload and an optional explicit ``id``.
    """
    messages: list[Message] = []
    for i, m in enumerate(raw_messages or []):
        if isinstance(m, str):
            messages.append(Message(id=f"{prefix}.{i}", default_text=_normalize_text(m)))
            continue
        if not isinstance(m, dict):
            continue
        explicit_id = m.get("id")
        raw_text = _message_text(m)
        mid = str(explicit_id) if explicit_id else f"{prefix}.{i}"
        default_text = _normalize_text(raw_text) if raw_text else ""
        messages.append(Message(id=mid, default_text=default_text))
    return messages


def _parse_set_params(raw: list[dict[str, Any]] | None) -> list[SetParameterAction]:
    return [
        SetParameterAction(parameter=item["parameter"], value=item.get("value"))
        for item in raw or []
    ]


def _parse_handoff(raw: dict[str, Any] | None) -> LiveAgentHandoff | None:
    if not raw:
        return None
    return LiveAgentHandoff(metadata=raw.get("metadata", {}) or {})


def _parse_fulfillment(raw: dict[str, Any] | None, prefix: str) -> Fulfillment:
    if not raw:
        return Fulfillment()
    return Fulfillment(
        messages=_parse_messages(raw.get("messages"), prefix),
        set_parameter_actions=_parse_set_params(
            _get(raw, "setParameterActions", "set_parameter_actions")
        ),
        webhook=_leaf(raw.get("webhook")),
        tag=raw.get("tag"),
        live_agent_handoff=_parse_handoff(
            _get(raw, "liveAgentHandoff", "live_agent_handoff")
        ),
    )


def _parse_event_handler(raw: dict[str, Any], prefix: str) -> EventHandler:
    event = raw["event"]
    return EventHandler(
        event=event,
        trigger_fulfillment=_parse_fulfillment(
            _get(raw, "triggerFulfillment", "trigger_fulfillment"),
            f"{prefix}.{event}",
        ),
    )


def _parse_route(raw: dict[str, Any], index: int) -> TransitionRoute:
    name = raw.get("name") or f"route-{index}"
    return TransitionRoute(
        name=name,
        intent=_leaf(raw.get("intent")),
        condition=raw.get("condition"),
        target_page=_leaf(_get(raw, "targetPage", "target_page")),
        target_flow=_leaf(_get(raw, "targetFlow", "target_flow")),
        trigger_fulfillment=_parse_fulfillment(
            _get(raw, "triggerFulfillment", "trigger_fulfillment"), name
        ),
    )


def _parse_validation(raw: dict[str, Any] | None) -> Validation | None:
    if not raw:
        return None
    return Validation(
        condition=raw["condition"],
        invalid_event=_get(raw, "invalidEvent", "invalid_event", default="sys.invalid-param"),
    )


def _parse_parameter(raw: dict[str, Any], page_name: str) -> Parameter:
    display_name = _get(raw, "displayName", "display_name")
    prefix = f"{page_name}.{display_name}"
    fill_raw = _get(raw, "fillBehavior", "fill_behavior", default={}) or {}
    reprompt_fulfillments = [
        _parse_fulfillment(f, f"{prefix}.reprompt.{j}")
        for j, f in enumerate(
            _get(fill_raw, "repromptFulfillments", "reprompt_fulfillments", default=[])
        )
    ]
    fill = FillBehavior(
        initial_prompt_fulfillment=_parse_fulfillment(
            _get(fill_raw, "initialPromptFulfillment", "initial_prompt_fulfillment"),
            f"{prefix}.initial",
        ),
        reprompt_event_handlers=[
            _parse_event_handler(h, f"{prefix}.reprompt")
            for h in _get(
                fill_raw, "repromptEventHandlers", "reprompt_event_handlers", default=[]
            )
        ],
        reprompt_fulfillments=reprompt_fulfillments,
    )
    return Parameter(
        display_name=display_name,
        entity_type=_leaf(_get(raw, "entityType", "entity_type")),
        required=bool(raw.get("required", False)),
        fill_behavior=fill,
        validation=_parse_validation(raw.get("validation")),
    )


def _parse_page(raw: dict[str, Any]) -> Page:
    display_name = _get(raw, "displayName", "display_name") or _leaf(raw.get("name"))
    form_raw = raw.get("form") or {}
    form = Form(
        parameters=[
            _parse_parameter(p, display_name) for p in form_raw.get("parameters", [])
        ]
    )
    return Page(
        display_name=display_name,
        entry_fulfillment=_parse_fulfillment(
            _get(raw, "entryFulfillment", "entry_fulfillment"), f"{display_name}.entry"
        ),
        form=form,
        transition_routes=[
            _parse_route(r, i)
            for i, r in enumerate(
                _get(raw, "transitionRoutes", "transition_routes", default=[])
            )
        ],
        event_handlers=[
            _parse_event_handler(h, f"{display_name}.event")
            for h in _get(raw, "eventHandlers", "event_handlers", default=[])
        ],
    )


def _parse_webhook(raw: dict[str, Any]) -> Webhook:
    gws = _get(raw, "genericWebService", "generic_web_service", default={}) or {}
    timeout = gws.get("timeout")
    timeout_seconds: float | None = None
    if isinstance(timeout, str) and timeout.endswith("s"):
        try:
            timeout_seconds = float(timeout[:-1])
        except ValueError:
            timeout_seconds = None
    return Webhook(
        display_name=_get(raw, "displayName", "display_name"),
        tag=raw["tag"],
        uri=gws.get("uri"),
        timeout_seconds=timeout_seconds,
    )


def parse_flow(data: dict[str, Any]) -> Flow:
    """Parse a raw Dialogflow CX flow dict into a :class:`Flow`."""
    nlu = _get(data, "nluSettings", "nlu_settings", default={}) or {}
    threshold = _get(nlu, "classificationThreshold", "classification_threshold", default=0.3)
    return Flow(
        display_name=_get(data, "displayName", "display_name"),
        description=data.get("description", ""),
        start_page=_leaf(_get(data, "startPage", "start_page")),
        pages=[_parse_page(p) for p in data.get("pages", [])],
        transition_routes=[
            _parse_route(r, i)
            for i, r in enumerate(
                _get(data, "transitionRoutes", "transition_routes", default=[])
            )
        ],
        event_handlers=[
            _parse_event_handler(h, "event")
            for h in _get(data, "eventHandlers", "event_handlers", default=[])
        ],
        intents=[
            TrainingIntent(
                display_name=_leaf(_get(i, "displayName", "display_name")),
                description=i.get("description", ""),
                training_phrases=_get(
                    i, "trainingPhrases", "training_phrases", default=[]
                ),
            )
            for i in data.get("intents", [])
        ],
        webhooks=[_parse_webhook(w) for w in data.get("webhooks", [])],
        classification_threshold=float(threshold),
    )


def load_flow(path: str | Path) -> Flow:
    """Load and parse a flow JSON file from disk."""
    text = Path(path).read_text(encoding="utf-8")
    return parse_flow(json.loads(text))
