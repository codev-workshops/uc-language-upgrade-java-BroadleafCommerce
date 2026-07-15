"""Live-agent handoff tool/event.

Mirrors the Dialogflow CX ``liveAgentHandoff`` response: it emits a structured
handoff signal (with a metadata ``reason``) that a channel gateway forwards to a
contact-center integration. In-process it is recorded on session state so the
flow engine and observability layer can react.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class HandoffSignal:
    reason: str
    metadata: dict[str, object] = field(default_factory=dict)

    def as_dict(self) -> dict[str, object]:
        return {"live_agent_handoff": True, "reason": self.reason, "metadata": self.metadata}


def live_agent_handoff(reason: str, metadata: dict[str, object] | None = None) -> dict[str, object]:
    """Trigger a live-agent handoff.

    Args:
        reason: Machine-readable reason (e.g. ``no_match_limit_reached``).
        metadata: Optional extra context (page, locale, order-related flags).
    """
    return HandoffSignal(reason=reason, metadata=metadata or {}).as_dict()
