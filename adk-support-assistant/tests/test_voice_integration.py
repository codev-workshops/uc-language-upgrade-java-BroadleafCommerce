"""STT -> flow and flow -> TTS integration tests."""

from adk_support_assistant.session.state import SessionState
from adk_support_assistant.voice.stt import NoopSttAdapter
from adk_support_assistant.voice.tts import NoopTtsAdapter


def test_stt_feeds_flow(engine):
    stt = NoopSttAdapter()
    state = SessionState(session_id="voice", locale="en")
    engine.start(state)
    # Audio payload here is just UTF-8 text bytes (noop adapter round-trips).
    transcript = stt.transcribe(b"where is my order", "en")
    assert transcript.text == "where is my order"
    result = engine.handle(state, transcript.text)
    assert result.current_page == "OrderLookupPage"


def test_flow_output_to_tts(engine):
    tts = NoopTtsAdapter()
    state = SessionState(session_id="voice2", locale="en")
    start = engine.start(state)
    reply = "\n".join(start.messages)
    audio = tts.synthesize(reply, state.locale)
    assert audio.data.decode("utf-8") == reply
    assert audio.locale == "en"


def test_stt_to_flow_to_tts_roundtrip(engine):
    stt, tts = NoopSttAdapter(), NoopTtsAdapter()
    state = SessionState(session_id="voice3", locale="es")
    engine.start(state)
    transcript = stt.transcribe("dónde está mi pedido".encode(), "es")
    result = engine.handle(state, transcript.text)
    audio = tts.synthesize("\n".join(result.messages), result.locale)
    assert audio.locale == "es"
    assert "pedido" in audio.data.decode("utf-8")
