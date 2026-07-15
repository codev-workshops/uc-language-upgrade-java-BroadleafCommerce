"""Multilingual parity: same scenario en vs es reaches the same pages."""

from adk_support_assistant.flow.engine import FlowEngine
from adk_support_assistant.nlu.classifier import RuleBasedNluClient
from adk_support_assistant.tools.order_lookup import MockOrderLookupClient


def _run(flow, bundles, locale, utterances):
    engine = FlowEngine(
        flow=flow,
        bundles=bundles,
        nlu_client=RuleBasedNluClient(flow),
        order_client=MockOrderLookupClient({"123456": "SHIPPED"}),
        default_locale="en",
        supported_locales=("en", "es"),
    )
    from adk_support_assistant.session.state import SessionState

    state = SessionState(session_id=f"parity-{locale}", locale=locale)
    engine.start(state)
    pages, ids, texts = [], [], []
    for utt in utterances:
        r = engine.handle(state, utt)
        pages.append(r.current_page)
        ids.append(tuple(r.message_ids))
        texts.append(tuple(r.messages))
    return pages, ids, texts


def test_en_es_reach_same_pages_and_ids(flow, bundles):
    en_utts = ["where is my order", "123456"]
    es_utts = ["dónde está mi pedido", "123456"]
    en_pages, en_ids, en_texts = _run(flow, bundles, "en", en_utts)
    es_pages, es_ids, es_texts = _run(flow, bundles, "es", es_utts)

    # Same navigation and same fulfillment ids...
    assert en_pages == es_pages
    assert en_ids == es_ids
    # ...but the rendered copy differs (localized).
    assert en_texts != es_texts
