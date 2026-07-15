"""REST/WebSocket channel gateway accepting text or audio."""

from .gateway import build_app, main

__all__ = ["build_app", "main"]
