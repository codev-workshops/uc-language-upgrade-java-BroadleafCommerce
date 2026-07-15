"""Integration test: drive the FlowAgent through a real ADK Runner.

Exercises the ``adk run`` code path (session state persistence, event yielding)
without any network/LLM by injecting a rule-based-NLU engine.
"""

import pytest
from google.adk.runners import InMemoryRunner
from google.genai import types

from adk_support_assistant.agent import FlowAgent
from adk_support_assistant.config import get_settings
from adk_support_assistant.flow.engine import FlowEngine
from adk_support_assistant.flow.loader import load_flow
from adk_support_assistant.i18n.bundle import load_bundles
from adk_support_assistant.nlu.classifier import RuleBasedNluClient
from adk_support_assistant.tools.order_lookup import MockOrderLookupClient

pytestmark = pytest.mark.asyncio


def _agent():
    flow = load_flow(get_settings().flow_file)
    engine = FlowEngine(
        flow=flow,
        bundles=load_bundles(),
        nlu_client=RuleBasedNluClient(flow),
        order_client=MockOrderLookupClient({"123456": "SHIPPED"}),
    )
    return FlowAgent(engine=engine)


async def _send(runner, user_id, session_id, text):
    replies = []
    async for event in runner.run_async(
        user_id=user_id,
        session_id=session_id,
        new_message=types.Content(role="user", parts=[types.Part(text=text)]),
    ):
        if event.content and event.content.parts:
            replies.append("".join(p.text or "" for p in event.content.parts))
    return "\n".join(replies)


async def test_agent_full_trajectory():
    agent = _agent()
    runner = InMemoryRunner(agent=agent, app_name="support")
    session = await runner.session_service.create_session(
        app_name="support", user_id="u1"
    )

    reply = await _send(runner, "u1", session.id, "123456")
    assert "shipped" in reply.lower()

    # State persisted on the session across turns.
    refreshed = await runner.session_service.get_session(
        app_name="support", user_id="u1", session_id=session.id
    )
    assert refreshed.state.get("flow_state", {}).get("current_page") == "FollowUpFlow"
