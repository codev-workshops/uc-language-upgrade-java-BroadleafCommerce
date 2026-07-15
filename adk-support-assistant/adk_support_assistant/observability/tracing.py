"""Structured per-turn tracing with PII redaction.

Each conversational turn records: input text, detected intent + entities, the
chosen route/target, tool calls, and latencies. Order IDs and person names are
redacted before anything is logged.
"""

from __future__ import annotations

import logging
import re
import time
from dataclasses import dataclass, field
from typing import Any

logger = logging.getLogger("adk_support_assistant.turn")

# 6-digit order numbers (and generally 4-10 digit runs) -> masked.
_ORDER_ID_RE = re.compile(r"\b\d{4,10}\b")
# Simple "my name is X" / "me llamo X" name capture.
_NAME_RE = re.compile(
    r"\b(?:my name is|i am|i'm|me llamo|soy|mi nombre es)\s+([A-Z][a-zà-ÿ]+)",
    re.IGNORECASE,
)


def _mask_order_id(match: re.Match[str]) -> str:
    digits = match.group(0)
    return "*" * (len(digits) - 2) + digits[-2:]


def redact(text: str | None, enabled: bool = True) -> str | None:
    """Redact order IDs and names from free text for safe logging."""
    if text is None or not enabled:
        return text
    redacted = _NAME_RE.sub(lambda m: m.group(0).replace(m.group(1), "[NAME]"), text)
    redacted = _ORDER_ID_RE.sub(_mask_order_id, redacted)
    return redacted


def redact_entities(entities: dict[str, Any], enabled: bool = True) -> dict[str, Any]:
    if not enabled:
        return dict(entities)
    out: dict[str, Any] = {}
    for key, value in entities.items():
        if key in {"order_id", "name", "customer_name"} and value is not None:
            s = str(value)
            out[key] = ("*" * max(len(s) - 2, 0)) + s[-2:] if len(s) > 2 else "**"
        else:
            out[key] = value
    return out


@dataclass
class TurnTrace:
    session_id: str
    redact_pii: bool = True
    input_text: str | None = None
    locale: str | None = None
    intent: str | None = None
    confidence: float | None = None
    entities: dict[str, Any] = field(default_factory=dict)
    from_page: str | None = None
    to_page: str | None = None
    chosen_route: str | None = None
    tool_calls: list[dict[str, Any]] = field(default_factory=list)
    events: list[str] = field(default_factory=list)
    emitted_message_ids: list[str] = field(default_factory=list)
    handoff: bool = False
    latencies_ms: dict[str, float] = field(default_factory=dict)
    _starts: dict[str, float] = field(default_factory=dict, repr=False)

    def start(self, name: str) -> None:
        self._starts[name] = time.perf_counter()

    def stop(self, name: str) -> None:
        if name in self._starts:
            self.latencies_ms[name] = round((time.perf_counter() - self._starts.pop(name)) * 1000, 2)

    def record_tool(self, name: str, args: dict[str, Any], duration_ms: float, ok: bool) -> None:
        self.tool_calls.append(
            {
                "name": name,
                "args": redact_entities(args, self.redact_pii),
                "duration_ms": round(duration_ms, 2),
                "ok": ok,
            }
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "session_id": self.session_id,
            "input": redact(self.input_text, self.redact_pii),
            "locale": self.locale,
            "intent": self.intent,
            "confidence": self.confidence,
            "entities": redact_entities(self.entities, self.redact_pii),
            "from_page": self.from_page,
            "to_page": self.to_page,
            "route": self.chosen_route,
            "events": self.events,
            "tool_calls": self.tool_calls,
            "handoff": self.handoff,
            "latencies_ms": self.latencies_ms,
        }

    def emit(self) -> None:
        logger.info("turn %s", self.to_dict())


class _Tracer:
    def __init__(self, redact_pii: bool = True):
        self.redact_pii = redact_pii

    def new_turn(self, session_id: str) -> TurnTrace:
        return TurnTrace(session_id=session_id, redact_pii=self.redact_pii)


_tracer: _Tracer | None = None


def get_tracer(redact_pii: bool = True) -> _Tracer:
    global _tracer
    if _tracer is None or _tracer.redact_pii != redact_pii:
        _tracer = _Tracer(redact_pii=redact_pii)
    return _tracer
