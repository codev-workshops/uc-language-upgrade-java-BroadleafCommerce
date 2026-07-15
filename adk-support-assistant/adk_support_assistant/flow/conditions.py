"""A SAFE evaluator for Dialogflow-CX-style condition expressions.

Supports a restricted grammar only. No Python ``eval``/``exec`` is used, so a
malicious flow (or user input reflected into state) cannot execute code.

Grammar (case-insensitive keywords)::

    expr    := or_expr
    or_expr := and_expr ( "OR" and_expr )*
    and_expr:= not_expr ( "AND" not_expr )*
    not_expr:= "NOT" not_expr | primary
    primary := "(" expr ")" | comparison | reference
    comparison := operand OP operand
    operand := reference | NUMBER | STRING | BOOL | NULL
    OP      := "=" | "==" | "!=" | "<" | "<=" | ">" | ">="
    reference := "$session.params." NAME
               | "$page.params." NAME
               | "$intent"
               | "$flow.params." NAME

A bare reference/operand (no operator) is truthy when it resolves to a
non-empty / non-null / non-zero value (mirrors CX "parameter is set" checks).
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

_TOKEN_RE = re.compile(
    r"""
    \s*(?:
        (?P<op><=|>=|!=|==|=|<|>)
      | (?P<lparen>\()
      | (?P<rparen>\))
      | (?P<string>"[^"]*"|'[^']*')
      | (?P<number>-?\d+(?:\.\d+)?)
      | (?P<ref>\$[A-Za-z_][A-Za-z0-9_.]*)
      | (?P<word>[A-Za-z_][A-Za-z0-9_]*)
    )
    """,
    re.VERBOSE,
)

_KEYWORDS = {"AND", "OR", "NOT", "TRUE", "FALSE", "NULL"}


class ConditionError(ValueError):
    """Raised when a condition string cannot be parsed."""


@dataclass
class _Tok:
    kind: str
    value: str


def _tokenize(text: str) -> list[_Tok]:
    tokens: list[_Tok] = []
    pos = 0
    while pos < len(text):
        if text[pos].isspace():
            pos += 1
            continue
        m = _TOKEN_RE.match(text, pos)
        if not m or m.end() == pos:
            raise ConditionError(f"Cannot tokenize condition at position {pos}: {text!r}")
        kind = m.lastgroup
        value = m.group(kind)
        pos = m.end()
        if kind == "word":
            upper = value.upper()
            if upper in _KEYWORDS:
                tokens.append(_Tok(upper, upper))
            else:
                # Bare identifiers are treated as unquoted string literals
                # (e.g. status values written without quotes).
                tokens.append(_Tok("string", value))
        else:
            tokens.append(_Tok(kind, value))
    return tokens


# ---------------------------------------------------------------------------
# AST nodes
# ---------------------------------------------------------------------------


class _Node:
    def eval(self, ctx: dict[str, Any]) -> Any:  # pragma: no cover - interface
        raise NotImplementedError


@dataclass
class _Literal(_Node):
    value: Any

    def eval(self, ctx: dict[str, Any]) -> Any:
        return self.value


@dataclass
class _Ref(_Node):
    path: str

    def eval(self, ctx: dict[str, Any]) -> Any:
        return _resolve_ref(self.path, ctx)


@dataclass
class _Compare(_Node):
    op: str
    left: _Node
    right: _Node

    def eval(self, ctx: dict[str, Any]) -> Any:
        return _apply_op(self.op, self.left.eval(ctx), self.right.eval(ctx))


@dataclass
class _Not(_Node):
    operand: _Node

    def eval(self, ctx: dict[str, Any]) -> Any:
        return not _truthy(self.operand.eval(ctx))


@dataclass
class _BoolOp(_Node):
    op: str  # AND / OR
    parts: list[_Node]

    def eval(self, ctx: dict[str, Any]) -> Any:
        if self.op == "AND":
            return all(_truthy(p.eval(ctx)) for p in self.parts)
        return any(_truthy(p.eval(ctx)) for p in self.parts)


@dataclass
class _Truthy(_Node):
    operand: _Node

    def eval(self, ctx: dict[str, Any]) -> Any:
        return _truthy(self.operand.eval(ctx))


def _truthy(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    if isinstance(value, str):
        return len(value) > 0
    if isinstance(value, (list, dict, tuple, set)):
        return len(value) > 0
    return True


def _resolve_ref(path: str, ctx: dict[str, Any]) -> Any:
    # path like "$session.params.order_id" or "$intent"
    body = path[1:]  # strip leading '$'
    if body == "intent":
        return ctx.get("intent")
    parts = body.split(".")
    if len(parts) >= 3 and parts[1] == "params":
        scope = parts[0]  # session / page / flow
        name = ".".join(parts[2:])
        scope_map = ctx.get(scope) or {}
        params = scope_map.get("params") if isinstance(scope_map, dict) else None
        if isinstance(params, dict):
            return params.get(name)
        return None
    # Unknown reference resolves to None (never raises, never executes).
    return None


def _coerce_compare(left: Any, right: Any) -> tuple[Any, Any]:
    """Coerce operands for comparison, allowing numeric strings vs numbers."""
    if isinstance(left, bool) or isinstance(right, bool):
        return left, right
    if isinstance(left, (int, float)) and isinstance(right, str):
        try:
            return left, type(left)(right) if right.strip() != "" else right
        except ValueError:
            return left, right
    if isinstance(right, (int, float)) and isinstance(left, str):
        try:
            return (type(right)(left) if left.strip() != "" else left), right
        except ValueError:
            return left, right
    return left, right


def _apply_op(op: str, left: Any, right: Any) -> bool:
    left, right = _coerce_compare(left, right)
    if op in ("=", "=="):
        return left == right
    if op == "!=":
        return left != right
    # Ordering comparisons require both sides comparable; None fails safely.
    if left is None or right is None:
        return False
    try:
        if op == "<":
            return left < right
        if op == "<=":
            return left <= right
        if op == ">":
            return left > right
        if op == ">=":
            return left >= right
    except TypeError:
        return False
    raise ConditionError(f"Unknown operator: {op}")


class _Parser:
    def __init__(self, tokens: list[_Tok]):
        self.tokens = tokens
        self.i = 0

    def _peek(self) -> _Tok | None:
        return self.tokens[self.i] if self.i < len(self.tokens) else None

    def _next(self) -> _Tok:
        tok = self.tokens[self.i]
        self.i += 1
        return tok

    def parse(self) -> _Node:
        node = self._or()
        if self.i != len(self.tokens):
            raise ConditionError("Trailing tokens in condition")
        return node

    def _or(self) -> _Node:
        parts = [self._and()]
        while self._peek() and self._peek().kind == "OR":
            self._next()
            parts.append(self._and())
        return parts[0] if len(parts) == 1 else _BoolOp("OR", parts)

    def _and(self) -> _Node:
        parts = [self._not()]
        while self._peek() and self._peek().kind == "AND":
            self._next()
            parts.append(self._not())
        return parts[0] if len(parts) == 1 else _BoolOp("AND", parts)

    def _not(self) -> _Node:
        if self._peek() and self._peek().kind == "NOT":
            self._next()
            return _Not(self._not())
        return self._primary()

    def _primary(self) -> _Node:
        tok = self._peek()
        if tok is None:
            raise ConditionError("Unexpected end of condition")
        if tok.kind == "lparen":
            self._next()
            node = self._or()
            closing = self._peek()
            if closing is None or closing.kind != "rparen":
                raise ConditionError("Missing closing parenthesis")
            self._next()
            return self._maybe_compare(node)
        operand = self._operand()
        return self._maybe_compare(operand)

    def _maybe_compare(self, left: _Node) -> _Node:
        tok = self._peek()
        if tok and tok.kind == "op":
            self._next()
            right = self._operand()
            return _Compare(tok.value, left, right)
        # Bare operand -> truthiness test.
        if isinstance(left, (_Ref, _Literal)):
            return _Truthy(left)
        return left

    def _operand(self) -> _Node:
        tok = self._next()
        if tok.kind == "ref":
            return _Ref(tok.value)
        if tok.kind == "number":
            return _Literal(float(tok.value) if "." in tok.value else int(tok.value))
        if tok.kind == "string":
            val = tok.value
            if val and val[0] in "\"'" and val[-1] == val[0]:
                val = val[1:-1]
            return _Literal(val)
        if tok.kind == "TRUE":
            return _Literal(True)
        if tok.kind == "FALSE":
            return _Literal(False)
        if tok.kind == "NULL":
            return _Literal(None)
        if tok.kind == "lparen":
            node = self._or()
            closing = self._peek()
            if closing is None or closing.kind != "rparen":
                raise ConditionError("Missing closing parenthesis")
            self._next()
            return node
        raise ConditionError(f"Unexpected token: {tok.kind} ({tok.value})")


def evaluate(condition: str | None, context: dict[str, Any]) -> bool:
    """Safely evaluate ``condition`` against ``context``.

    ``context`` shape::

        {
          "session": {"params": {...}},
          "page": {"params": {...}},
          "flow": {"params": {...}},
          "intent": "Intent_Cancel_Order",
        }

    An empty / None condition is treated as always-true (unconditional route).
    Parse errors raise :class:`ConditionError`.
    """
    if condition is None or condition.strip() == "":
        return True
    tokens = _tokenize(condition)
    if not tokens:
        return True
    ast = _Parser(tokens).parse()
    return _truthy(ast.eval(context))
