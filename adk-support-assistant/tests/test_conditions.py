"""Tests for the SAFE condition evaluator."""

import pytest

from adk_support_assistant.flow.conditions import ConditionError, evaluate

CTX = {
    "session": {"params": {"order_id": 123456, "order_status": "SHIPPED", "flag": True}},
    "page": {"params": {"status": "FINAL"}},
    "flow": {"params": {}},
    "intent": "Intent_Cancel_Order",
}


@pytest.mark.parametrize(
    "cond,expected",
    [
        ("$session.params.order_id >= 100000 AND $session.params.order_id <= 999999", True),
        ("$session.params.order_id >= 100000 AND $session.params.order_id <= 100", False),
        ('$page.params.status = "FINAL" AND $session.params.order_status = "SHIPPED"', True),
        ('$session.params.order_status = "PENDING"', False),
        ('$session.params.order_status != "PENDING"', True),
        ("$session.params.order_id", True),
        ("$session.params.missing", False),
        ("$session.params.flag", True),
        ('NOT ($session.params.order_status = "PENDING")', True),
        ("$session.params.order_id = 123456 OR $session.params.order_status = \"PENDING\"", True),
        ("", True),
        (None, True),
        ('$intent = "Intent_Cancel_Order"', True),
    ],
)
def test_evaluate(cond, expected):
    assert evaluate(cond, CTX) is expected


def test_numeric_string_coercion():
    ctx = {"session": {"params": {"order_id": "123456"}}, "page": {"params": {}}}
    assert evaluate("$session.params.order_id >= 100000", ctx) is True


def test_missing_reference_is_falsey_not_error():
    assert evaluate("$session.params.nope > 5", {"session": {"params": {}}}) is False


def test_no_code_execution():
    # Attempting to smuggle Python raises a parse error rather than executing.
    with pytest.raises(ConditionError):
        evaluate("__import__('os').system('echo hi')", CTX)


def test_unbalanced_parens_error():
    with pytest.raises(ConditionError):
        evaluate("($session.params.order_id > 1", CTX)
