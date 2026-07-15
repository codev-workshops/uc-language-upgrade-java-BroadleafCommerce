"""Shared pytest fixtures."""

from __future__ import annotations

from pathlib import Path

import pytest

from adk_support_assistant.config import get_settings
from adk_support_assistant.flow.engine import FlowEngine
from adk_support_assistant.flow.loader import load_flow
from adk_support_assistant.flow.schema import Flow
from adk_support_assistant.i18n.bundle import load_bundles
from adk_support_assistant.nlu.classifier import RuleBasedNluClient
from adk_support_assistant.session.state import SessionState
from adk_support_assistant.tools.order_lookup import MockOrderLookupClient

PROJECT_ROOT = Path(__file__).resolve().parents[1]
EVAL_DIR = PROJECT_ROOT / "eval"
DATA_DIR = Path(__file__).resolve().parent / "data"


@pytest.fixture
def flow() -> Flow:
    return load_flow(get_settings().flow_file)


@pytest.fixture
def bundles():
    return load_bundles()


@pytest.fixture
def order_client() -> MockOrderLookupClient:
    return MockOrderLookupClient()


@pytest.fixture
def engine(flow, bundles, order_client) -> FlowEngine:
    return FlowEngine(
        flow=flow,
        bundles=bundles,
        nlu_client=RuleBasedNluClient(flow),
        order_client=order_client,
        default_locale="en",
        supported_locales=("en", "es"),
        redact_pii=True,
    )


@pytest.fixture
def new_state():
    def _make(locale: str | None = "en", session_id: str = "test") -> SessionState:
        return SessionState(session_id=session_id, locale=locale)

    return _make
