"""No-match escalation to live-agent handoff."""

from adk_support_assistant.tools.handoff import live_agent_handoff


def test_two_no_matches_escalate(engine, new_state):
    state = new_state()
    engine.start(state)
    engine.handle(state, "track my delivery")  # -> OrderLookupPage form
    first = engine.handle(state, "blah blah")
    assert first.handoff is False
    assert "form.order_id.reprompt_1" in first.message_ids
    second = engine.handle(state, "more nonsense")
    assert second.handoff is True
    assert "event.handoff" in second.message_ids
    assert state.handoff_reason == "order_id_no_match"


def test_page_level_no_match_on_welcome(engine, new_state):
    state = new_state()
    engine.start(state)
    r1 = engine.handle(state, "asdfghjkl")
    assert "event.no_match_1" in r1.message_ids
    r2 = engine.handle(state, "qwertyuiop")
    assert r2.handoff is True


def test_live_agent_handoff_tool():
    signal = live_agent_handoff("no_match_limit_reached", {"page": "WelcomePage"})
    assert signal["live_agent_handoff"] is True
    assert signal["reason"] == "no_match_limit_reached"
    assert signal["metadata"]["page"] == "WelcomePage"
