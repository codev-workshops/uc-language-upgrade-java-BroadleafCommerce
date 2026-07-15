"""Tests for the deterministic flow engine (driven by the real CX export).

The supplied ``OrderManagementFlow`` collects ``order_id`` via a form on
``WelcomePage``, validates it with condition transition routes, runs the
``fetchOrderStatus`` webhook on ``OrderLookupPage`` entry, then routes on the
resulting ``order_status``. ``SHIPPED`` targets ``FollowUpFlow`` and ``PENDING``
targets ``ModifyOrderPage`` \u2014 neither is defined in the partial export, so both
are treated as terminal targets.
"""


def test_start_renders_welcome(engine, new_state):
    state = new_state()
    result = engine.start(state)
    assert result.current_page == "WelcomePage"
    assert "WelcomePage.entry.0" in result.message_ids
    assert "WelcomePage.order_id.initial.0" in result.message_ids


def test_valid_shipped_routes_to_follow_up(engine, new_state, order_client):
    order_client.set_status("123456", "SHIPPED")
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "123456")
    assert result.current_page == "FollowUpFlow"
    assert "route-status-shipped.0" in result.message_ids
    assert state.session_params["order_status"] == "SHIPPED"
    assert state.session_params["is_valid_order"] is True
    # carrier came from the webhook and is interpolated into the message.
    assert any("UPS" in m for m in result.messages)


def test_valid_pending_routes_to_modify(engine, new_state, order_client):
    order_client.set_status("654321", "SUBMITTED")
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "654321")
    assert result.current_page == "ModifyOrderPage"
    assert "route-status-pending.0" in result.message_ids
    assert state.session_params["order_status"] == "PENDING"


def test_invalid_order_id_returns_to_welcome(engine, new_state):
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "99999")  # 5 digits -> invalid route
    assert result.current_page == "WelcomePage"
    assert "route-conditional-invalid.0" in result.message_ids
    # invalid route clears order_id so the form re-prompts.
    assert state.session_params.get("order_id") is None
    assert "WelcomePage.order_id.initial.0" in result.message_ids


def test_invalid_then_valid_recovers(engine, new_state, order_client):
    order_client.set_status("123456", "SHIPPED")
    state = new_state()
    engine.start(state)
    engine.handle(state, "12345")  # invalid
    result = engine.handle(state, "123456")  # valid
    assert result.current_page == "FollowUpFlow"
    assert "route-status-shipped.0" in result.message_ids


def test_order_id_boundaries(engine, new_state, order_client):
    order_client.set_status("100000", "SUBMITTED")
    order_client.set_status("999999", "SHIPPED")
    for value, expected_page in [("100000", "ModifyOrderPage"), ("999999", "FollowUpFlow")]:
        state = new_state()
        engine.start(state)
        result = engine.handle(state, value)
        assert result.current_page == expected_page, value


def test_not_found_stays_on_lookup_without_crash(engine, new_state):
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "555555")  # valid format, not in mock data
    assert result.current_page == "OrderLookupPage"
    assert state.session_params["order_status"] == "NOT_FOUND"
    assert result.handoff is False


def test_webhook_failure_falls_back_to_handoff(engine, new_state, order_client):
    def boom(order_id):
        raise RuntimeError("erp down")

    order_client.lookup = boom  # type: ignore[assignment]
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "123456")
    assert result.handoff is True
    assert "system.error.lookup_failed" in result.message_ids


def test_global_cancel_route(engine, new_state):
    state = new_state()
    engine.start(state)
    result = engine.handle(state, "please cancel my order")
    assert result.intent == "Intent_Cancel_Order"
    assert result.current_page == "MainMenu"
    assert "route-global-intent-cancel.0" in result.message_ids
