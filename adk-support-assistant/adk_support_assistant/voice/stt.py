"""Speech-to-text adapter interface + a no-op implementation.

Real engines (Whisper, Vosk, an in-house ASR service, etc.) implement
:class:`SttAdapter` and are selected via ``STT_ENGINE``. Locale is threaded
through so the engine can pick the right acoustic/language model.
"""

from __future__ import annotations

import abc
from dataclasses import dataclass


@dataclass(frozen=True)
class Transcript:
    text: str
    locale: str
    confidence: float = 1.0


class SttAdapter(abc.ABC):
    @abc.abstractmethod
    def transcribe(self, audio: bytes, locale: str) -> Transcript:
        """Transcribe ``audio`` (raw PCM/opus/wav bytes) in ``locale``."""
        raise NotImplementedError


class NoopSttAdapter(SttAdapter):
    """Test/dev adapter.

    Interprets the audio payload as UTF-8 text if decodable (so text-channel
    tests can exercise the STT path), otherwise returns an empty transcript.
    """

    def transcribe(self, audio: bytes, locale: str) -> Transcript:
        try:
            text = audio.decode("utf-8")
        except (UnicodeDecodeError, AttributeError):
            text = ""
        return Transcript(text=text, locale=locale, confidence=1.0 if text else 0.0)


def build_stt(engine: str = "noop") -> SttAdapter:
    if engine == "noop":
        return NoopSttAdapter()
    raise ValueError(f"Unknown STT engine: {engine!r}")
