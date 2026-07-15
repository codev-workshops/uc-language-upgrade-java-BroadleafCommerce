"""Adapter pointing Google ADK / LiteLLM at an internal OpenAI-compatible endpoint.

The in-house model is exposed through LiteLLM's ``openai/<model>`` provider, so
any server implementing the OpenAI ``/v1/chat/completions`` contract works. The
same configuration powers both the ADK ``LiteLlm`` model (used by agents) and a
thin :func:`complete_json` helper (used by the NLU classifier for structured
output without requiring a full ADK Runner).
"""

from __future__ import annotations

import json
from typing import Any

from ..config import LlmConfig, get_settings


def build_in_house_llm(config: LlmConfig | None = None) -> Any:
    """Build an ADK ``LiteLlm`` model bound to the in-house endpoint.

    Imported lazily so importing this module never requires google-adk at import
    time (keeps unit tests that only use the flow engine dependency-light).
    """
    from google.adk.models.lite_llm import LiteLlm

    cfg = config or get_settings().llm
    kwargs: dict[str, Any] = {"model": cfg.model}
    if cfg.api_base:
        kwargs["api_base"] = cfg.api_base
    if cfg.api_key:
        kwargs["api_key"] = cfg.api_key
    kwargs["temperature"] = cfg.temperature
    kwargs["max_tokens"] = cfg.max_tokens
    kwargs["timeout"] = cfg.timeout_seconds
    return LiteLlm(**kwargs)


def complete_json(
    messages: list[dict[str, str]],
    config: LlmConfig | None = None,
    schema: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Call the in-house LLM and parse a strict-JSON object response.

    Uses LiteLLM directly with ``response_format={"type": "json_object"}`` so the
    model is constrained to emit a JSON object. Raises ``ValueError`` if the
    response is not valid JSON.
    """
    import litellm

    cfg = config or get_settings().llm
    params: dict[str, Any] = {
        "model": cfg.model,
        "messages": messages,
        "temperature": cfg.temperature,
        "max_tokens": cfg.max_tokens,
        "timeout": cfg.timeout_seconds,
        "response_format": {"type": "json_object"},
    }
    if cfg.api_base:
        params["api_base"] = cfg.api_base
    if cfg.api_key:
        params["api_key"] = cfg.api_key
    if schema is not None:
        params["response_format"] = {
            "type": "json_schema",
            "json_schema": {"name": "nlu_result", "schema": schema, "strict": True},
        }

    response = litellm.completion(**params)
    content = response["choices"][0]["message"]["content"]
    try:
        return json.loads(content)
    except (json.JSONDecodeError, TypeError) as exc:
        raise ValueError(f"LLM did not return valid JSON: {content!r}") from exc
