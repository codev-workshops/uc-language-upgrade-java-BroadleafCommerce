"""No-match escalation to live-agent handoff (flow-level event handlers)."""

from adk_support_assistant.tools.handoff import live_agent_handoff


def test_two_no_matches_escalate(engine, new_state):
    state = new_state()
    engine.start(state)  # WelcomePage form awaits order_id
    first = engine.handle(state, "blah blah")
    assert first.handoff is False
    # First no-match reprompts with the form's reprompt fulfillment.
    assert "WelcomePage.order_id.reprompt.0.0" in first.message_ids
    second = engine.handle(state, "more nonsense")
    assert second.handoff is True
    assert "event.sys.no-match-2.0" in second.message_ids
    assert state.handoff_reason == "Max consecutive no-match reached"
    assert state.handoff_metadata == {"reason": "Max consecutive no-match reached"}


def test_handoff_uses_flow_level_event_handler(engine, new_state):
    """WelcomePage has no page-level handlers; the flow-level ones fire."""
    state = new_state()
    engine.start(state)
    engine.handle(state, "???")
    result = engine.handle(state, "!!!")
    assert result.handoff is True
    assert result.trace is not None
    assert "sys.no-match-2" in result.trace.events


def test_live_agent_handoff_tool():
    signal = live_agent_handoff("no_match_limit_reached", {"page": "WelcomePage"})
    assert signal["live_agent_handoff"] is True
    assert signal["reason"] == "no_match_limit_reached"
    assert signal["metadata"]["page"] == "WelcomePage"
