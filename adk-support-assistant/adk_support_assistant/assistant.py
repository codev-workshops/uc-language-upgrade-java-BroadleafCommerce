"""High-level assembly: build a ready-to-use :class:`FlowEngine` from config."""

from __future__ import annotations

from .config import Settings, get_settings
from .flow.engine import FlowEngine
from .flow.loader import load_flow
from .flow.schema import Flow
from .i18n.bundle import ResourceBundle, load_bundles
from .nlu.classifier import LlmNluClient, NluClient, RuleBasedNluClient
from .tools.order_lookup import OrderLookupClient, build_client


def build_engine(
    settings: Settings | None = None,
    nlu_client: NluClient | None = None,
    order_client: OrderLookupClient | None = None,
    use_llm_nlu: bool = True,
) -> FlowEngine:
    """Build a fully wired flow engine.

    Args:
        settings: overrides; defaults to environment-derived settings.
        nlu_client: inject a custom NLU client (tests use RuleBasedNluClient).
        order_client: inject a custom order-lookup client (tests use the mock).
        use_llm_nlu: when True and no ``nlu_client`` given, use the in-house LLM;
            otherwise fall back to the deterministic rule-based classifier.
    """
    cfg = settings or get_settings()
    flow: Flow = load_flow(cfg.flow_file)
    bundles: dict[str, ResourceBundle] = load_bundles()

    if nlu_client is None:
        nlu_client = (
            LlmNluClient(flow, cfg.llm) if use_llm_nlu else RuleBasedNluClient(flow)
        )
    if order_client is None:
        order_client = build_client(cfg.order_lookup)

    return FlowEngine(
        flow=flow,
        bundles=bundles,
        nlu_client=nlu_client,
        order_client=order_client,
        default_locale=cfg.default_locale,
        supported_locales=cfg.supported_locales,
        redact_pii=cfg.redact_pii,
    )
