"""Per-locale resource bundles keyed by fulfillment id, plus language detection.

The flow structure is language-independent; all user-facing copy lives in
``i18n/<locale>.json`` files keyed by the stable fulfillment ``id``. Rendering a
message = bundle lookup + ``str.format`` interpolation of session params.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from ..config import I18N_DIR


class MissingMessageError(KeyError):
    """Raised when a fulfillment id is absent from a bundle."""


@dataclass(frozen=True)
class ResourceBundle:
    locale: str
    messages: dict[str, str]

    def has(self, message_id: str) -> bool:
        return message_id in self.messages

    def render(self, message_id: str, params: dict[str, Any] | None = None) -> str:
        try:
            template = self.messages[message_id]
        except KeyError as exc:
            raise MissingMessageError(
                f"Message id {message_id!r} not found for locale {self.locale!r}"
            ) from exc
        if not params:
            return template
        return _safe_format(template, params)


def _safe_format(template: str, params: dict[str, Any]) -> str:
    """Format ``{name}`` placeholders, leaving unknown placeholders intact."""

    def repl(match: re.Match[str]) -> str:
        key = match.group(1)
        if key in params and params[key] is not None:
            return str(params[key])
        return match.group(0)

    return re.sub(r"\{([A-Za-z_][A-Za-z0-9_]*)\}", repl, template)


def load_bundles(i18n_dir: str | Path | None = None) -> dict[str, ResourceBundle]:
    """Load every ``<locale>.json`` bundle from ``i18n_dir``."""
    directory = Path(i18n_dir) if i18n_dir else I18N_DIR
    bundles: dict[str, ResourceBundle] = {}
    for path in sorted(directory.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        locale = data.get("locale", path.stem)
        messages = data.get("messages", {})
        bundles[locale] = ResourceBundle(locale=locale, messages=messages)
    return bundles


# ---------------------------------------------------------------------------
# Lightweight, dependency-free language detection (en / es).
# ---------------------------------------------------------------------------

_ES_MARKERS = {
    "hola",
    "pedido",
    "cancelar",
    "estado",
    "gracias",
    "por",
    "favor",
    "quiero",
    "mi",
    "el",
    "número",
    "numero",
    "dónde",
    "donde",
    "está",
    "esta",
    "necesito",
    "ayuda",
    "buenos",
    "días",
    "dias",
    "sí",
}
_ES_CHARS = set("ñáéíóú¿¡ü")

_EN_MARKERS = {
    "hello",
    "order",
    "cancel",
    "status",
    "the",
    "please",
    "where",
    "is",
    "my",
    "want",
    "need",
    "help",
    "thanks",
    "shipped",
    "track",
    "yes",
    "number",
}


def detect_locale(
    text: str,
    supported: tuple[str, ...] = ("en", "es"),
    default: str = "en",
) -> str:
    """Heuristically detect ``en`` vs ``es`` from a short utterance.

    Deterministic and offline so it is safe in tests. For production-grade
    detection swap this for a model-based detector behind the same signature.
    """
    if not text or not text.strip():
        return default if default in supported else supported[0]
    lowered = text.lower()
    tokens = set(re.findall(r"[\wáéíóúñü¿¡]+", lowered))

    es_score = len(tokens & _ES_MARKERS)
    en_score = len(tokens & _EN_MARKERS)
    if any(ch in _ES_CHARS for ch in lowered):
        es_score += 2

    if "es" in supported and es_score > en_score:
        return "es"
    if "en" in supported and en_score >= es_score and en_score > 0:
        return "en"
    return default if default in supported else supported[0]
