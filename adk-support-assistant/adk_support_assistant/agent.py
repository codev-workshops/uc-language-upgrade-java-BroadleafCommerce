"""ADK-compatible root agent (``adk run`` / ``adk web``).

The deterministic :class:`FlowEngine` drives the conversation. We wrap it in a
custom ``BaseAgent`` (``FlowAgent``) that, on each user turn, restores session
state from the ADK session, runs the engine, persists updated state via an
``EventActions.state_delta``, and yields the assistant's reply as an ``Event``.

``adk run adk_support_assistant`` and ``adk web`` both discover ``root_agent``.
"""

from __future__ import annotations

from collections.abc import AsyncGenerator

from google.adk.agents import BaseAgent
from google.adk.agents.invocation_context import InvocationContext
from google.adk.events import Event, EventActions
from google.genai import types

from .assistant import build_engine
from .flow.engine import FlowEngine
from .i18n.bundle import detect_locale
from .session.state import SessionState

_STATE_KEY = "flow_state"


class FlowAgent(BaseAgent):
    """Bridges ADK's turn loop to the deterministic flow engine."""

    model_config = {"arbitrary_types_allowed": True, "extra": "allow"}

    def __init__(self, engine: FlowEngine | None = None, **kwargs):
        super().__init__(name=kwargs.pop("name", "order_management_assistant"), **kwargs)
        object.__setattr__(self, "_engine", engine or build_engine())

    @property
    def engine(self) -> FlowEngine:
        return self._engine

    def _load_state(self, ctx: InvocationContext) -> SessionState:
        raw = dict(ctx.session.state or {})
        stored = raw.get(_STATE_KEY)
        if stored:
            return SessionState.from_adk_state(ctx.session.id, stored)
        return SessionState(session_id=ctx.session.id)

    def _user_text(self, ctx: InvocationContext) -> str:
        content = ctx.user_content
        if content and content.parts:
            return "".join(p.text or "" for p in content.parts)
        return ""

    async def _run_async_impl(
        self, ctx: InvocationContext
    ) -> AsyncGenerator[Event, None]:
        state = self._load_state(ctx)
        user_text = self._user_text(ctx)

        messages: list[str] = []
        if not state.started:
            # Detect locale from the opening utterance if provided.
            if user_text.strip():
                state.locale = detect_locale(
                    user_text,
                    self.engine.supported_locales,
                    self.engine.default_locale,
                )
            start_result = self.engine.start(state)
            messages.extend(start_result.messages)

        if user_text.strip():
            turn = self.engine.handle(state, user_text)
            messages.extend(turn.messages)
            handoff = turn.handoff
        else:
            handoff = state.handoff

        reply = "\n".join(m for m in messages if m)
        actions = EventActions(
            state_delta={_STATE_KEY: state.to_adk_state()},
            escalate=handoff,
        )
        yield Event(
            author=self.name,
            content=types.Content(role="model", parts=[types.Part(text=reply)]),
            actions=actions,
            turn_complete=True,
        )


def build_root_agent() -> FlowAgent:
    return FlowAgent()


# Discovered by `adk run` / `adk web`.
root_agent = build_root_agent()
