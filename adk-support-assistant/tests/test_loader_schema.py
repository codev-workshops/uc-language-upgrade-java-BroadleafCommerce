"""Tests for the Dialogflow CX loader and schema (real export)."""

from adk_support_assistant.flow.engine import FETCH_ORDER_STATUS_TAG
from adk_support_assistant.flow.loader import load_flow, parse_flow


def test_flow_loads(flow):
    assert flow.display_name == "OrderManagementFlow"
    assert flow.start_page == "WelcomePage"
    # The supplied export only defines these two pages; ModifyOrderPage /
    # FollowUpFlow / MainMenu are referenced but not defined.
    assert {p.display_name for p in flow.pages} == {"WelcomePage", "OrderLookupPage"}


def test_intents_are_derived_from_routes(flow):
    # No intent catalog is declared; the only referenced intent is the global
    # cancel route, normalised from its fully-qualified resource name.
    assert flow.intent_names == ["Intent_Cancel_Order"]


def test_entry_webhook_on_order_lookup(flow):
    # The export has no webhook registry; the webhook fires from the page's
    # entry fulfillment via its tag.
    assert flow.webhook_by_tag("fetchOrderStatus") is None
    entry = flow.page("OrderLookupPage").entry_fulfillment
    assert entry.tag == FETCH_ORDER_STATUS_TAG
    assert entry.webhook == "ERP_Lookup_Service"


def test_order_id_form_on_welcome(flow):
    page = flow.page("WelcomePage")
    params = page.form.parameters
    assert len(params) == 1
    order_id = params[0]
    assert order_id.display_name == "order_id"
    assert order_id.entity_type == "sys.number"
    assert order_id.required is True
    # Validation is expressed via transition routes, not a parameter validator.
    assert order_id.validation is None
    assert order_id.fill_behavior.reprompt_fulfillments


def test_flow_level_routes_and_events(flow):
    cancel = next(r for r in flow.transition_routes if r.intent == "Intent_Cancel_Order")
    assert cancel.target_page == "MainMenu"
    assert flow.event_handler("sys.no-match-1") is not None
    handoff = flow.event_handler("sys.no-match-2")
    assert handoff is not None
    assert handoff.trigger_fulfillment.live_agent_handoff is not None
    assert handoff.trigger_fulfillment.live_agent_handoff.metadata == {
        "reason": "Max consecutive no-match reached"
    }


def test_condition_routes_carry_targets(flow):
    lookup = flow.page("OrderLookupPage")
    targets = {r.name: (r.target_page, r.target_flow) for r in lookup.transition_routes}
    assert targets["route-status-shipped"] == (None, "FollowUpFlow")
    assert targets["route-status-pending"] == ("ModifyOrderPage", None)


def test_inline_text_preserved_as_default(flow):
    entry = flow.page("WelcomePage").entry_fulfillment
    assert entry.messages[0].default_text.startswith("Welcome to our automated assistant")


def test_loader_accepts_snake_case():
    raw = {
        "display_name": "MiniFlow",
        "start_page": "Home",
        "pages": [
            {
                "display_name": "Home",
                "entry_fulfillment": {"messages": [{"id": "home.entry"}]},
                "transition_routes": [{"intent": "Intent_Foo", "target_page": "Home"}],
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
