"""FastAPI gateway exposing the assistant over text (REST/WS) and audio.

Endpoints:
  * ``POST /session``                 -> create a session, returns greeting.
  * ``POST /session/{id}/text``       -> send a text turn.
  * ``POST /session/{id}/audio``      -> send an audio turn (STT -> flow -> TTS).
  * ``GET  /healthz``                 -> liveness.
  * ``WS   /ws/{id}``                 -> bidirectional text turns.

Sessions are held in an in-memory store (swap for Redis/DB in production).
"""

from __future__ import annotations

import base64
from dataclasses import dataclass

from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel

from ..assistant import build_engine
from ..config import Settings, get_settings
from ..flow.engine import FlowEngine, TurnResult
from ..session.state import SessionState
from ..voice.stt import SttAdapter, build_stt
from ..voice.tts import TtsAdapter, build_tts


class TextTurn(BaseModel):
    text: str
    locale: str | None = None


class AudioTurn(BaseModel):
    audio_base64: str
    locale: str | None = None


class TurnResponse(BaseModel):
    session_id: str
    reply: str
    messages: list[str]
    current_page: str | None
    locale: str
    intent: str | None = None
    handoff: bool = False
    handoff_reason: str | None = None
    audio_base64: str | None = None


@dataclass
class _SessionEntry:
    state: SessionState


class SessionStore:
    def __init__(self) -> None:
        self._sessions: dict[str, _SessionEntry] = {}

    def create(self, session_id: str, locale: str | None) -> SessionState:
        state = SessionState(session_id=session_id, locale=locale)
        self._sessions[session_id] = _SessionEntry(state=state)
        return state

    def get(self, session_id: str) -> SessionState:
        entry = self._sessions.get(session_id)
        if entry is None:
            raise KeyError(session_id)
        return entry.state


def _to_response(session_id: str, result: TurnResult, audio_b64: str | None = None) -> TurnResponse:
    return TurnResponse(
        session_id=session_id,
        reply="\n".join(m for m in result.messages if m),
        messages=result.messages,
        current_page=result.current_page,
        locale=result.locale,
        intent=result.intent,
        handoff=result.handoff,
        handoff_reason=result.handoff_reason,
        audio_base64=audio_b64,
    )


def build_app(
    engine: FlowEngine | None = None,
    settings: Settings | None = None,
    stt: SttAdapter | None = None,
    tts: TtsAdapter | None = None,
    store: SessionStore | None = None,
) -> FastAPI:
    cfg = settings or get_settings()
    engine = engine or build_engine(cfg)
    stt = stt or build_stt(cfg.voice.stt_engine)
    tts = tts or build_tts(cfg.voice.tts_engine)
    store = store or SessionStore()

    app = FastAPI(title="ADK Support Assistant", version="0.1.0")

    import itertools

    counter = itertools.count(1)

    @app.get("/healthz")
    def healthz() -> dict[str, str]:
        return {"status": "ok"}

    @app.post("/session", response_model=TurnResponse)
    def create_session(payload: TextTurn | None = None) -> TurnResponse:
        session_id = f"s-{next(counter)}"
        locale = payload.locale if payload else None
        state = store.create(session_id, locale)
        result = engine.start(state)
        return _to_response(session_id, result)

    @app.post("/session/{session_id}/text", response_model=TurnResponse)
    def text_turn(session_id: str, payload: TextTurn) -> TurnResponse:
        try:
            state = store.get(session_id)
        except KeyError as exc:
            raise HTTPException(status_code=404, detail="unknown session") from exc
        if payload.locale and state.locale is None:
            state.locale = payload.locale
        result = engine.handle(state, payload.text)
        return _to_response(session_id, result)

    @app.post("/session/{session_id}/audio", response_model=TurnResponse)
    def audio_turn(session_id: str, payload: AudioTurn) -> TurnResponse:
        try:
            state = store.get(session_id)
        except KeyError as exc:
            raise HTTPException(status_code=404, detail="unknown session") from exc
        audio_bytes = base64.b64decode(payload.audio_base64)
        locale = payload.locale or state.locale or engine.default_locale
        transcript = stt.transcribe(audio_bytes, locale)
        if state.locale is None:
            state.locale = transcript.locale
        result = engine.handle(state, transcript.text)
        reply = "\n".join(m for m in result.messages if m)
        audio = tts.synthesize(reply, result.locale)
        return _to_response(session_id, result, base64.b64encode(audio.data).decode("ascii"))

    @app.websocket("/ws/{session_id}")
    async def ws_endpoint(websocket: WebSocket, session_id: str) -> None:
        await websocket.accept()
        try:
            state = store.get(session_id)
        except KeyError:
            state = store.create(session_id, None)
            start = engine.start(state)
            await websocket.send_json(_to_response(session_id, start).model_dump())
        try:
            while True:
                data = await websocket.receive_json()
                text = data.get("text", "")
                if data.get("locale") and state.locale is None:
                    state.locale = data["locale"]
                result = engine.handle(state, text)
                await websocket.send_json(_to_response(session_id, result).model_dump())
        except WebSocketDisconnect:
            return

    return app


def main() -> None:  # pragma: no cover - process entry point
    import uvicorn

    uvicorn.run(build_app(), host="0.0.0.0", port=8000)


app = None  # lazily built by ASGI servers via `create_app`


def create_app() -> FastAPI:  # pragma: no cover - ASGI factory
    return build_app()
