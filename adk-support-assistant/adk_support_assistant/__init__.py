"""Data-driven, multilingual text+voice support assistant built on Google ADK.

Replicates a Dialogflow CX flow (OrderManagementFlow) using a deterministic
flow engine, an in-house-LLM NLU agent, and pluggable order-lookup / voice
adapters.
"""

__version__ = "0.1.0"


def __getattr__(name: str):
    # Lazily expose ``root_agent`` for `adk run` / `adk web` discovery without
    # forcing google-adk import for lightweight flow-engine unit tests.
    if name == "root_agent":
        from .agent import root_agent

        return root_agent
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
