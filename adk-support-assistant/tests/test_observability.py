"""PII redaction and per-turn tracing."""

from adk_support_assistant.observability.tracing import (
    TurnTrace,
    redact,
    redact_entities,
)


def test_redact_order_id():
    assert redact("my order is 123456") == "my order is ****56"


def test_redact_name():
    out = redact("my name is Alice")
    assert "Alice" not in out and "[NAME]" in out


def test_redact_disabled():
    assert redact("order 123456", enabled=False) == "order 123456"


def test_redact_entities_masks_order_id():
    masked = redact_entities({"order_id": 123456, "intent": "x"})
    assert masked["order_id"] == "****56"
    assert masked["intent"] == "x"


def test_turn_trace_dict_is_redacted():
    trace = TurnTrace(session_id="s", redact_pii=True)
    trace.input_text = "cancel order 654321"
    trace.entities = {"order_id": 654321}
    d = trace.to_dict()
    assert "654321" not in d["input"]
    assert d["entities"]["order_id"] == "****21"


def test_turn_trace_records_tool_and_latency():
    trace = TurnTrace(session_id="s")
    trace.start("nlu")
    trace.stop("nlu")
    trace.record_tool("fetchOrderStatus", {"order_id": 123456}, 12.3, ok=True)
    d = trace.to_dict()
    assert "nlu" in d["latencies_ms"]
    assert d["tool_calls"][0]["name"] == "fetchOrderStatus"
    assert d["tool_calls"][0]["args"]["order_id"] == "****56"
