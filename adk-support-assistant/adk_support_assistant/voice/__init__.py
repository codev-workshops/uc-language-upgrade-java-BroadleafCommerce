"""Swappable speech-to-text and text-to-speech adapters."""

from .stt import NoopSttAdapter, SttAdapter, build_stt
from .tts import NoopTtsAdapter, TtsAdapter, build_tts

__all__ = [
    "SttAdapter",
    "TtsAdapter",
    "NoopSttAdapter",
    "NoopTtsAdapter",
    "build_stt",
    "build_tts",
]
