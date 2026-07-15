"""Intent classification + entity extraction.

Two interchangeable clients implement the :class:`NluClient` protocol:

  * :class:`LlmNluClient` - production: calls the in-house LLM and validates the
    STRICT-JSON response against :class:`NluResult`.
  * :class:`RuleBasedNluClient` - deterministic, offline: keyword/regex matching
    used for CI golden-dataset tests and as a graceful fallback when the LLM is
    unavailable.

The in-house LLM can additionally be wrapped as an ADK ``LlmAgent`` via
:func:`build_nlu_agent`.
"""

from __future__ import annotations

import re
from typing import Protocol

from ..config import LlmConfig
from ..flow.schema import Flow
from .schema import OUT_OF_SCOPE, NluResult


def build_intent_catalog(flow: Flow) -> list[str]:
    """Return the intent names declared by the flow (excludes OUT_OF_SCOPE)."""
    return list(flow.intent_names)


class NluClient(Protocol):
    def classify(self, text: str, locale: str) -> NluResult:  # pragma: no cover - protocol
        ...


# ---------------------------------------------------------------------------
# Entity extraction helpers (shared).
# ---------------------------------------------------------------------------

_NUMBER_RE = re.compile(r"\b(\d{4,8})\b")


def extract_order_id(text: str) -> int | None:
    """Extract the first plausible order-number-like integer from ``text``."""
    match = _NUMBER_RE.search(text.replace(" ", "").replace("-", "")) or _NUMBER_RE.search(text)
    if match:
        return int(match.group(1))
    return None


# ---------------------------------------------------------------------------
# LLM-backed classifier.
# ---------------------------------------------------------------------------

_SYSTEM_PROMPT = """You are an intent classification and entity extraction engine for a customer-support assistant.
Classify the user's message into exactly ONE intent from this catalog:
{catalog}
If the message does not match any intent, use "{oos}".
Extract entities when present. For order numbers, put an integer under "order_id".
The user's locale is "{locale}"; reply in the same structured format regardless of language.
Respond with ONLY a JSON object matching this schema (no prose):
{{"intent": <string>, "confidence": <0..1 float>, "entities": {{...}}, "locale": "{locale}"}}"""


class LlmNluClient:
    """Classifies utterances using the in-house LLM with strict JSON output."""

    def __init__(self, flow: Flow, config: LlmConfig | None = None):
        self._flow = flow
        self._catalog = build_intent_catalog(flow)
        self._config = config

    def _system_prompt(self, locale: str) -> str:
        return _SYSTEM_PROMPT.format(
            catalog="\n".join(f"- {name}" for name in self._catalog),
            oos=OUT_OF_SCOPE,
            locale=locale,
        )

    def classify(self, text: str, locale: str) -> NluResult:
        from ..llm.in_house_llm import complete_json

        messages = [
            {"role": "system", "content": self._system_prompt(locale)},
            {"role": "user", "content": text},
        ]
        schema = NluResult.json_schema_for_llm(self._catalog)
        try:
            raw = complete_json(messages, config=self._config, schema=schema)
            result = NluResult.model_validate(raw)
        except Exception:  # noqa: BLE001 - fall back deterministically on any LLM error
            return RuleBasedNluClient(self._flow).classify(text, locale)
        if result.intent not in self._catalog and result.intent != OUT_OF_SCOPE:
            result = result.model_copy(update={"intent": OUT_OF_SCOPE})
        # Ensure order_id entity is captured even if the model missed it.
        if "order_id" not in result.entities:
            oid = extract_order_id(text)
            if oid is not None:
                result.entities["order_id"] = oid
        return result


# ---------------------------------------------------------------------------
# Rule-based classifier (deterministic, offline, multilingual).
# ---------------------------------------------------------------------------

# Keyword sets per intent per language. Deliberately small; extend via config.
_INTENT_KEYWORDS: dict[str, dict[str, list[str]]] = {
    "Intent_Cancel_Order": {
        "en": ["cancel", "stop", "call off", "don't want", "do not want", "refund"],
        "es": ["cancelar", "cancela", "anular", "detener", "ya no quiero", "devoluci"],
    },
    "Intent_Check_Order_Status": {
        "en": ["status", "where is", "track", "shipped", "arriving", "delivery", "when will"],
        "es": ["estado", "dónde está", "donde esta", "rastrear", "enviado", "seguimiento", "cuándo llega", "cuando llega"],
    },
    "Intent_Provide_Order_Id": {
        "en": ["order number", "order id", "it is", "number is", "my order is"],
        "es": ["número de pedido", "numero de pedido", "es el", "mi pedido es", "el número es", "el numero es"],
    },
}


class RuleBasedNluClient:
    """Deterministic keyword/regex classifier used offline and as fallback."""

    def __init__(self, flow: Flow):
        self._flow = flow
        self._catalog = build_intent_catalog(flow)

    def classify(self, text: str, locale: str) -> NluResult:
        lowered = text.lower().strip()
        entities: dict[str, object] = {}
        oid = extract_order_id(text)
        if oid is not None:
            entities["order_id"] = oid

        best_intent = OUT_OF_SCOPE
        best_score = 0
        for intent in self._catalog:
            keyword_langs = _INTENT_KEYWORDS.get(intent, {})
            keywords = keyword_langs.get(locale, []) + keyword_langs.get("en", [])
            score = sum(1 for kw in keywords if kw in lowered)
            if score > best_score:
                best_score = score
                best_intent = intent

        # A bare order number with no verb is "provide order id".
        if best_intent == OUT_OF_SCOPE and oid is not None and len(lowered) <= 12:
            if "Intent_Provide_Order_Id" in self._catalog:
                best_intent = "Intent_Provide_Order_Id"
                best_score = 1

        confidence = 0.0 if best_intent == OUT_OF_SCOPE else min(0.5 + 0.25 * best_score, 0.99)
        return NluResult(
            intent=best_intent,
            confidence=confidence,
            entities=entities,
            locale=locale,
        )


def build_nlu_agent(flow: Flow, config: LlmConfig | None = None):
    """Build an ADK ``LlmAgent`` that performs NLU with the in-house LLM.

    Returned lazily (imports google-adk) so pure flow-engine tests stay light.
    """
    from google.adk.agents import LlmAgent

    from ..llm.in_house_llm import build_in_house_llm

    catalog = build_intent_catalog(flow)
    instruction = (
        "Classify the user's message into one intent from: "
        + ", ".join(catalog)
        + f", or {OUT_OF_SCOPE}. Extract entities (order_id as integer). "
        "Respond ONLY with a JSON object: "
        '{"intent": str, "confidence": float, "entities": object, "locale": str}.'
    )
    return LlmAgent(
        name="nlu_classifier",
        model=build_in_house_llm(config),
        instruction=instruction,
        output_key="nlu_result",
    )
