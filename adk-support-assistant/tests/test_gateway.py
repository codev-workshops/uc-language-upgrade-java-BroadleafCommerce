"""Channel gateway (REST + audio) tests."""

import base64

import pytest
from fastapi.testclient import TestClient

from adk_support_assistant.channel.gateway import build_app
from adk_support_assistant.config import get_settings
from adk_support_assistant.flow.engine import FlowEngine
from adk_support_assistant.flow.loader import load_flow
from adk_support_assistant.i18n.bundle import load_bundles
from adk_support_assistant.nlu.classifier import RuleBasedNluClient
from adk_support_assistant.tools.order_lookup import MockOrderLookupClient


@pytest.fixture
def client():
    flow = load_flow(get_settings().flow_file)
    engine = FlowEngine(
        flow=flow,
        bundles=load_bundles(),
        nlu_client=RuleBasedNluClient(flow),
        order_client=MockOrderLookupClient({"123456": "SHIPPED"}),
    )
    return TestClient(build_app(engine=engine))


def test_healthz(client):
    assert client.get("/healthz").json() == {"status": "ok"}


def test_text_conversation(client):
    created = client.post("/session", json={"text": "", "locale": "en"}).json()
    sid = created["session_id"]
    assert created["current_page"] == "WelcomePage"

    r1 = client.post(f"/session/{sid}/text", json={"text": "123456"}).json()
    assert r1["current_page"] == "FollowUpFlow"
    assert "shipped" in r1["reply"].lower()


def test_audio_turn(client):
    sid = client.post("/session", json={"text": "", "locale": "en"}).json()["session_id"]
    audio_b64 = base64.b64encode(b"123456").decode("ascii")
    resp = client.post(f"/session/{sid}/audio", json={"audio_base64": audio_b64}).json()
    assert resp["current_page"] == "FollowUpFlow"
    assert resp["audio_base64"] is not None


def test_unknown_session_404(client):
    resp = client.post("/session/nope/text", json={"text": "hi"})
    assert resp.status_code == 404


def test_websocket_text(client):
    with client.websocket_connect("/ws/ws-1") as ws:
        greeting = ws.receive_json()
        assert greeting["current_page"] == "WelcomePage"
        ws.send_json({"text": "123456"})
        reply = ws.receive_json()
        assert reply["current_page"] == "FollowUpFlow"
