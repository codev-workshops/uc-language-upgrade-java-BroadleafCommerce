"""Strict output schema for NLU results."""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field

#: Catch-all intent for utterances outside the flow's intent catalog.
OUT_OF_SCOPE = "OUT_OF_SCOPE"


class NluResult(BaseModel):
    """Structured NLU output validated against a JSON schema.

    ``entities`` holds extracted slot values, e.g. ``{"order_id": 123456}``.
    """

    model_config = ConfigDict(extra="ignore")

    intent: str = Field(description="Detected intent name or OUT_OF_SCOPE.")
    confidence: float = Field(ge=0.0, le=1.0, default=0.0)
    entities: dict[str, object] = Field(default_factory=dict)
    locale: str = Field(default="en")

    @property
    def is_out_of_scope(self) -> bool:
        return self.intent == OUT_OF_SCOPE

    @staticmethod
    def json_schema_for_llm(intent_names: list[str]) -> dict[str, object]:
        """A minimal JSON schema fed to the LLM for structured output."""
        return {
            "type": "object",
            "additionalProperties": False,
            "properties": {
                "intent": {"type": "string", "enum": [*intent_names, OUT_OF_SCOPE]},
                "confidence": {"type": "number", "minimum": 0.0, "maximum": 1.0},
                "entities": {
                    "type": "object",
                    "additionalProperties": True,
                    "properties": {
                        "order_id": {"type": ["integer", "string", "null"]},
                    },
                },
                "locale": {"type": "string"},
            },
            "required": ["intent", "confidence", "entities", "locale"],
        }
