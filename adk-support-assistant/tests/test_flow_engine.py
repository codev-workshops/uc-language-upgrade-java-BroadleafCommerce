"""Tests for the deterministic flow engine."""


def test_start_renders_welcome(engine, new_state):
    state = new_state()
    result = engine.start(state)
    assert result.current_page == "WelcomePage"
    assert "welcome.entry" in result.message_ids


def test_check_status_transitions_to_lookup(engine, new_state):
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "where is my order")
    assert result.current_page == "OrderLookupPage"
    assert "form.order_id.initial" in result.message_ids


def test_invalid_order_id_reprompts(engine, new_state):
    state = new_state()
    engine.start(state)
    engine.handle(state, "where is my order")
    result = engine.handle(state, "99999")
    assert result.current_page == "OrderLookupPage"
    assert "form.order_id.invalid" in result.message_ids
    assert "order_id" not in state.session_params


def test_valid_shipped_routes_to_modify(engine, new_state, order_client):
    order_client.set_status("123456", "SHIPPED")
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    result = engine.handle(state, "123456")
    assert result.current_page == "ModifyOrderPage"
    assert "lookup.result.shipped" in result.message_ids
    assert state.session_params["order_status"] == "SHIPPED"


def test_valid_pending_routes_to_modify(engine, new_state, order_client):
    order_client.set_status("654321", "SUBMITTED")
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    result = engine.handle(state, "654321")
    assert result.current_page == "ModifyOrderPage"
    assert "lookup.result.pending" in result.message_ids


def test_not_found_returns_to_welcome(engine, new_state):
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    result = engine.handle(state, "555555")  # not in mock data
    assert "lookup.result.not_found" in result.message_ids
    assert result.current_page == "WelcomePage"


def test_cancel_pending_succeeds(engine, new_state, order_client):
    order_client.set_status("654321", "SUBMITTED")
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    engine.handle(state, "654321")
    result = engine.handle(state, "cancel it")
    assert "modify.cancel.success" in result.message_ids


def test_cancel_shipped_too_late(engine, new_state, order_client):
    order_client.set_status("123456", "SHIPPED")
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    engine.handle(state, "123456")
    result = engine.handle(state, "cancel it")
    assert "modify.cancel.too_late" in result.message_ids


def test_webhook_failure_falls_back_to_handoff(engine, new_state, order_client):
    def boom(order_id):
        raise RuntimeError("erp down")

    order_client.lookup = boom  # type: ignore[assignment]
    state = new_state()
    engine.start(state)
    engine.handle(state, "check my order status")
    result = engine.handle(state, "123456")
    assert result.handoff is True
    assert "system.error.lookup_failed" in result.message_ids
