"""In-house LLM adapter (OpenAI-compatible via LiteLlm)."""

from .in_house_llm import build_in_house_llm, complete_json

__all__ = ["build_in_house_llm", "complete_json"]
