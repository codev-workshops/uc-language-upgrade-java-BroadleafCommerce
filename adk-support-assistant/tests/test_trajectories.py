"""Replay ADK-style .test.json conversation trajectories through the engine.

Each ``eval/*.test.json`` file defines an ordered list of turns with expected
target page, emitted fulfillment ids, handoff status and session params. The
harness runs them deterministically with the rule-based NLU and the in-memory
order client (seeded from the file's ``order_data``).
"""

import json

import pytest

from adk_support_assistant.flow.engine import FlowEngine
from adk_support_assistant.flow.loader import load_flow
from adk_support_assistant.i18n.bundle import load_bundles
from adk_support_assistant.nlu.classifier import RuleBasedNluClient
from adk_support_assistant.session.state import SessionState
from adk_support_assistant.tools.order_lookup import MockOrderLookupClient

from .conftest import EVAL_DIR

TRAJECTORIES = sorted(EVAL_DIR.glob("*.test.json"))


def _engine(flow, order_data):
    return FlowEngine(
        flow=flow,
        bundles=load_bundles(),
        nlu_client=RuleBasedNluClient(flow),
        order_client=MockOrderLookupClient(order_data or None),
        default_locale="en",
        supported_locales=("en", "es"),
    )


@pytest.mark.parametrize("path", TRAJECTORIES, ids=lambda p: p.stem)
def test_trajectory(path):
    from adk_support_assistant.config import get_settings

    spec = json.loads(path.read_text(encoding="utf-8"))
    flow = load_flow(get_settings().flow_file)
    engine = _engine(flow, spec.get("order_data"))
    state = SessionState(session_id=spec["eval_set_id"], locale=spec.get("locale", "en"))

    for i, turn in enumerate(spec["turns"]):
        if turn.get("user") is None:
            result = engine.start(state)
        else:
            result = engine.handle(state, turn["user"])

        ctx = f"{path.stem} turn {i} (user={turn.get('user')!r})"

        if "expect_page" in turn:
            assert result.current_page == turn["expect_page"], (
                f"{ctx}: page {result.current_page} != {turn['expect_page']}"
            )
        for mid in turn.get("expect_message_ids", []):
            assert mid in result.message_ids, f"{ctx}: missing message id {mid} in {result.message_ids}"
        if "expect_handoff" in turn:
            assert result.handoff is turn["expect_handoff"], f"{ctx}: handoff mismatch"
        if "expect_handoff_reason" in turn:
            assert state.handoff_reason == turn["expect_handoff_reason"], f"{ctx}: reason mismatch"
        if "expect_intent" in turn:
            assert result.intent == turn["expect_intent"], f"{ctx}: intent {result.intent}"
        for key, value in turn.get("expect_session_params", {}).items():
            assert state.session_params.get(key) == value, (
                f"{ctx}: session param {key}={state.session_params.get(key)} != {value}"
            )


def test_all_trajectories_present():
    assert len(TRAJECTORIES) >= 4
