"""Parse a Dialogflow-CX-style flow export into the internal :mod:`schema`.

The loader is tolerant of the two shapes Dialogflow CX uses:
  * ``camelCase`` keys (as exported by the CX API / gcloud), and
  * already-normalised ``snake_case`` keys.

It never invents user-facing copy; message ``id`` fields become i18n keys.
"""

from __future__ import annotations

import json
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


def _get(d: dict[str, Any], *keys: str, default: Any = None) -> Any:
    """Return the first present key among ``keys`` (camelCase or snake_case)."""
    for key in keys:
        if key in d and d[key] is not None:
            return d[key]
    return default


def _parse_messages(raw_messages: list[dict[str, Any]] | None) -> list[Message]:
    messages: list[Message] = []
    for m in raw_messages or []:
        mid = m.get("id")
        if mid is None:
            # Fall back to the text payload's first entry as the key.
            text = _get(m, "text", default={})
            texts = text.get("text") if isinstance(text, dict) else None
            mid = texts[0] if texts else None
        if mid is not None:
            messages.append(Message(id=str(mid)))
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


def _parse_fulfillment(raw: dict[str, Any] | None) -> Fulfillment:
    if not raw:
        return Fulfillment()
    return Fulfillment(
        messages=_parse_messages(raw.get("messages")),
        set_parameter_actions=_parse_set_params(
            _get(raw, "setParameterActions", "set_parameter_actions")
        ),
        webhook=raw.get("webhook"),
        tag=raw.get("tag"),
        live_agent_handoff=_parse_handoff(
            _get(raw, "liveAgentHandoff", "live_agent_handoff")
        ),
    )


def _parse_event_handler(raw: dict[str, Any]) -> EventHandler:
    return EventHandler(
        event=raw["event"],
        trigger_fulfillment=_parse_fulfillment(
            _get(raw, "triggerFulfillment", "trigger_fulfillment")
        ),
    )


def _parse_route(raw: dict[str, Any]) -> TransitionRoute:
    return TransitionRoute(
        intent=raw.get("intent"),
        condition=raw.get("condition"),
        target_page=_get(raw, "targetPage", "target_page"),
        target_flow=_get(raw, "targetFlow", "target_flow"),
        trigger_fulfillment=_parse_fulfillment(
            _get(raw, "triggerFulfillment", "trigger_fulfillment")
        ),
    )


def _parse_validation(raw: dict[str, Any] | None) -> Validation | None:
    if not raw:
        return None
    return Validation(
        condition=raw["condition"],
        invalid_event=_get(raw, "invalidEvent", "invalid_event", default="sys.invalid-param"),
    )


def _parse_parameter(raw: dict[str, Any]) -> Parameter:
    fill_raw = _get(raw, "fillBehavior", "fill_behavior", default={}) or {}
    fill = FillBehavior(
        initial_prompt_fulfillment=_parse_fulfillment(
            _get(fill_raw, "initialPromptFulfillment", "initial_prompt_fulfillment")
        ),
        reprompt_event_handlers=[
            _parse_event_handler(h)
            for h in _get(
                fill_raw, "repromptEventHandlers", "reprompt_event_handlers", default=[]
            )
        ],
    )
    return Parameter(
        display_name=_get(raw, "displayName", "display_name"),
        entity_type=_get(raw, "entityType", "entity_type"),
        required=bool(raw.get("required", False)),
        fill_behavior=fill,
        validation=_parse_validation(raw.get("validation")),
    )


def _parse_page(raw: dict[str, Any]) -> Page:
    form_raw = raw.get("form") or {}
    form = Form(
        parameters=[_parse_parameter(p) for p in form_raw.get("parameters", [])]
    )
    return Page(
        display_name=_get(raw, "displayName", "display_name"),
        entry_fulfillment=_parse_fulfillment(
            _get(raw, "entryFulfillment", "entry_fulfillment")
        ),
        form=form,
        transition_routes=[
            _parse_route(r)
            for r in _get(raw, "transitionRoutes", "transition_routes", default=[])
        ],
        event_handlers=[
            _parse_event_handler(h)
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
        start_page=_get(data, "startPage", "start_page"),
        pages=[_parse_page(p) for p in data.get("pages", [])],
        intents=[
            TrainingIntent(
                display_name=_get(i, "displayName", "display_name"),
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
