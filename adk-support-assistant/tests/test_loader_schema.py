"""Tests for the Dialogflow CX loader and schema."""

from adk_support_assistant.flow.loader import load_flow, parse_flow


def test_flow_loads(flow):
    assert flow.display_name == "OrderManagementFlow"
    assert flow.start_page == "WelcomePage"
    assert {p.display_name for p in flow.pages} == {
        "WelcomePage",
        "OrderLookupPage",
        "ModifyOrderPage",
    }


def test_intents_and_webhook(flow):
    assert "Intent_Cancel_Order" in flow.intent_names
    wh = flow.webhook_by_tag("fetchOrderStatus")
    assert wh is not None and wh.display_name == "ERP_Lookup_Service"


def test_order_id_form_parameter(flow):
    page = flow.page("OrderLookupPage")
    params = page.form.parameters
    assert len(params) == 1
    order_id = params[0]
    assert order_id.display_name == "order_id"
    assert order_id.entity_type == "sys.number"
    assert order_id.required is True
    assert order_id.validation is not None
    assert "100000" in order_id.validation.condition


def test_event_handlers_present(flow):
    page = flow.page("WelcomePage")
    assert page.event_handler("sys.no-match-1") is not None
    handoff = page.event_handler("sys.no-match-2")
    assert handoff is not None
    assert handoff.trigger_fulfillment.live_agent_handoff is not None


def test_loader_accepts_snake_case():
    raw = {
        "display_name": "MiniFlow",
        "start_page": "Home",
        "pages": [
            {
                "display_name": "Home",
                "entry_fulfillment": {"messages": [{"id": "home.entry"}]},
                "transition_routes": [
                    {"intent": "Intent_Foo", "target_page": "Home"}
                ],
            }
        ],
    }
    flow = parse_flow(raw)
    assert flow.display_name == "MiniFlow"
    assert flow.page("Home").entry_fulfillment.message_ids == ["home.entry"]


def test_reload_is_stable(flow):
    from adk_support_assistant.config import get_settings

    again = load_flow(get_settings().flow_file)
    assert again.display_name == flow.display_name
