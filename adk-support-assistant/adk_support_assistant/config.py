"""Environment-driven configuration for the support assistant."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

from dotenv import load_dotenv

# Load a local .env if present (no-op in production where env is set directly).
load_dotenv(override=False)

PACKAGE_ROOT = Path(__file__).resolve().parent
PROJECT_ROOT = PACKAGE_ROOT.parent
FLOWS_DIR = PROJECT_ROOT / "flows"
I18N_DIR = PROJECT_ROOT / "i18n"


def _bool(name: str, default: bool) -> bool:
    val = os.getenv(name)
    if val is None:
        return default
    return val.strip().lower() in {"1", "true", "yes", "on"}


def _float(name: str, default: float) -> float:
    val = os.getenv(name)
    if val is None or val.strip() == "":
        return default
    try:
        return float(val)
    except ValueError:
        return default


def _int(name: str, default: int) -> int:
    val = os.getenv(name)
    if val is None or val.strip() == "":
        return default
    try:
        return int(val)
    except ValueError:
        return default


@dataclass(frozen=True)
class LlmConfig:
    model: str = field(default_factory=lambda: os.getenv("INHOUSE_LLM_MODEL", "openai/inhouse-gpt"))
    api_base: str | None = field(default_factory=lambda: os.getenv("INHOUSE_LLM_API_BASE"))
    api_key: str | None = field(default_factory=lambda: os.getenv("INHOUSE_LLM_API_KEY"))
    temperature: float = field(default_factory=lambda: _float("INHOUSE_LLM_TEMPERATURE", 0.0))
    max_tokens: int = field(default_factory=lambda: _int("INHOUSE_LLM_MAX_TOKENS", 512))
    timeout_seconds: int = field(default_factory=lambda: _int("INHOUSE_LLM_TIMEOUT_SECONDS", 30))


@dataclass(frozen=True)
class OrderLookupConfig:
    client: str = field(default_factory=lambda: os.getenv("ORDER_LOOKUP_CLIENT", "mock"))
    base_url: str = field(default_factory=lambda: os.getenv("ORDER_LOOKUP_BASE_URL", "http://localhost:8080"))
    path: str = field(
        default_factory=lambda: os.getenv(
            "ORDER_LOOKUP_PATH", "/api/support/orders/{order_id}/status"
        )
    )
    api_key: str | None = field(default_factory=lambda: os.getenv("ORDER_LOOKUP_API_KEY") or None)
    timeout_seconds: float = field(default_factory=lambda: _float("ORDER_LOOKUP_TIMEOUT_SECONDS", 5.0))


@dataclass(frozen=True)
class VoiceConfig:
    stt_engine: str = field(default_factory=lambda: os.getenv("STT_ENGINE", "noop"))
    tts_engine: str = field(default_factory=lambda: os.getenv("TTS_ENGINE", "noop"))


@dataclass(frozen=True)
class Settings:
    llm: LlmConfig = field(default_factory=LlmConfig)
    order_lookup: OrderLookupConfig = field(default_factory=OrderLookupConfig)
    voice: VoiceConfig = field(default_factory=VoiceConfig)
    default_locale: str = field(default_factory=lambda: os.getenv("DEFAULT_LOCALE", "en"))
    supported_locales: tuple[str, ...] = field(
        default_factory=lambda: tuple(
            loc.strip()
            for loc in os.getenv("SUPPORTED_LOCALES", "en,es").split(",")
            if loc.strip()
        )
    )
    log_level: str = field(default_factory=lambda: os.getenv("LOG_LEVEL", "INFO"))
    redact_pii: bool = field(default_factory=lambda: _bool("REDACT_PII", True))
    flow_file: Path = field(default_factory=lambda: FLOWS_DIR / "order_management_flow.json")


def get_settings() -> Settings:
    """Build a fresh Settings from the current environment.

    Not cached so tests can monkeypatch env vars between calls.
    """
    return Settings()
