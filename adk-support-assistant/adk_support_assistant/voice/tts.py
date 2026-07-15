"""Text-to-speech adapter interface + a no-op implementation.

Real engines (Coqui, Piper, an in-house TTS service, etc.) implement
:class:`TtsAdapter` and are selected via ``TTS_ENGINE``. Locale selects the
voice/language.
"""

from __future__ import annotations

import abc
from dataclasses import dataclass


@dataclass(frozen=True)
class Audio:
    data: bytes
    locale: str
    mime_type: str = "audio/wav"


class TtsAdapter(abc.ABC):
    @abc.abstractmethod
    def synthesize(self, text: str, locale: str) -> Audio:
        """Synthesize ``text`` into audio for ``locale``."""
        raise NotImplementedError


class NoopTtsAdapter(TtsAdapter):
    """Test/dev adapter that encodes text as UTF-8 bytes (round-trippable)."""

    def synthesize(self, text: str, locale: str) -> Audio:
        return Audio(data=text.encode("utf-8"), locale=locale, mime_type="text/plain")


def build_tts(engine: str = "noop") -> TtsAdapter:
    if engine == "noop":
        return NoopTtsAdapter()
    raise ValueError(f"Unknown TTS engine: {engine!r}")
