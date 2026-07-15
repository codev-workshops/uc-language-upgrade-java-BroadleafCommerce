"""Tests for order_id validation and the fetchOrderStatus tool."""

import httpx
import pytest

from adk_support_assistant.flow.conditions import evaluate
from adk_support_assistant.tools.order_lookup import (
    STATUS_NOT_FOUND,
    STATUS_PENDING,
    STATUS_SHIPPED,
    HttpOrderLookupClient,
    MockOrderLookupClient,
    fetch_order_status,
    map_backend_status,
    set_default_client,
)

VALIDATION = "$session.params.order_id >= 100000 AND $session.params.order_id <= 999999"


@pytest.mark.parametrize(
    "value,valid",
    [(100000, True), (999999, True), (500000, True), (99999, False), (1000000, False), (0, False)],
)
def test_order_id_validation(value, valid):
    ctx = {"session": {"params": {"order_id": value}}, "page": {"params": {}}}
    assert evaluate(VALIDATION, ctx) is valid


@pytest.mark.parametrize(
    "raw,expected",
    [
        ("SHIPPED", STATUS_SHIPPED),
        ("submitted", STATUS_PENDING),
        ("IN_PROCESS", STATUS_PENDING),
        ("CANCELLED", STATUS_NOT_FOUND),
        (None, STATUS_NOT_FOUND),
        ("weird", STATUS_PENDING),
    ],
)
def test_map_backend_status(raw, expected):
    assert map_backend_status(raw) == expected


def test_mock_client_found_and_not_found():
    client = MockOrderLookupClient({"123456": "SHIPPED"})
    ok = client.lookup("123456")
    assert ok.found and ok.status == STATUS_SHIPPED
    assert ok.carrier == "UPS"  # carrier reported for shipped orders
    missing = client.lookup("000000")
    assert not missing.found and missing.status == STATUS_NOT_FOUND


def test_fetch_order_status_tool_uses_default_client():
    set_default_client(MockOrderLookupClient({"111111": "SUBMITTED"}))
    try:
        result = fetch_order_status("111111")
        assert result["status"] == STATUS_PENDING
        assert result["found"] is True
    finally:
        set_default_client(None)


def test_http_client_maps_response():
    def handler(request: httpx.Request) -> httpx.Response:
        assert "123456" in str(request.url)
        return httpx.Response(
            200,
            json={"orderNumber": "123456", "status": "SHIPPED", "found": True, "carrier": "FedEx"},
        )

    transport = httpx.MockTransport(handler)
    from adk_support_assistant.config import OrderLookupConfig

    cfg = OrderLookupConfig(client="http", base_url="http://erp", path="/orders/{order_id}")
    client = HttpOrderLookupClient(cfg, client=httpx.Client(transport=transport))
    result = client.lookup("123456")
    assert result.status == STATUS_SHIPPED and result.found
    assert result.carrier == "FedEx"


def test_http_client_404_is_not_found():
    transport = httpx.MockTransport(lambda req: httpx.Response(404))
    from adk_support_assistant.config import OrderLookupConfig

    cfg = OrderLookupConfig(client="http", base_url="http://erp", path="/orders/{order_id}")
    client = HttpOrderLookupClient(cfg, client=httpx.Client(transport=transport))
    result = client.lookup("123456")
    assert result.status == STATUS_NOT_FOUND and not result.found


def test_http_client_transport_error_returns_error():
    def boom(req: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("down", request=req)

    transport = httpx.MockTransport(boom)
    from adk_support_assistant.config import OrderLookupConfig

    cfg = OrderLookupConfig(client="http", base_url="http://erp", path="/orders/{order_id}")
    client = HttpOrderLookupClient(cfg, client=httpx.Client(transport=transport))
    result = client.lookup("123456")
    assert result.error is not None and not result.found
