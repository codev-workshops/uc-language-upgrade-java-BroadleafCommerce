"""Mutable per-conversation session state for the flow engine.

Kept as a plain dataclass (not tied to ADK) so the engine is unit-testable in
isolation. The ADK integration layer mirrors these fields into the ADK session
``state`` dict.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class SessionState:
    session_id: str = "local"
    locale: str | None = None
    current_page: str | None = None
    session_params: dict[str, Any] = field(default_factory=dict)
    page_params: dict[str, Any] = field(default_factory=dict)
    no_match_count: int = 0
    handoff: bool = False
    handoff_reason: str | None = None
    handoff_metadata: dict[str, Any] = field(default_factory=dict)
    started: bool = False

    def condition_context(self, intent: str | None = None) -> dict[str, Any]:
        """Build the context dict consumed by the safe condition evaluator."""
        return {
            "session": {"params": self.session_params},
            "page": {"params": self.page_params},
            "flow": {"params": self.session_params},
            "intent": intent,
        }

    def reset_page_form(self) -> None:
        self.page_params = {}

    def to_adk_state(self) -> dict[str, Any]:
        return {
            "locale": self.locale,
            "current_page": self.current_page,
            "session_params": dict(self.session_params),
            "page_params": dict(self.page_params),
            "no_match_count": self.no_match_count,
            "handoff": self.handoff,
            "handoff_reason": self.handoff_reason,
        }

    @classmethod
    def from_adk_state(cls, session_id: str, state: dict[str, Any]) -> SessionState:
        return cls(
            session_id=session_id,
            locale=state.get("locale"),
            current_page=state.get("current_page"),
            session_params=dict(state.get("session_params", {})),
            page_params=dict(state.get("page_params", {})),
            no_match_count=int(state.get("no_match_count", 0)),
            handoff=bool(state.get("handoff", False)),
            handoff_reason=state.get("handoff_reason"),
            started=state.get("current_page") is not None,
        )
