"""Tests for i18n bundles and language detection."""

import pytest

from adk_support_assistant.i18n.bundle import detect_locale, load_bundles


def test_bundles_have_parity():
    bundles = load_bundles()
    assert set(bundles) >= {"en", "es"}
    en_keys = set(bundles["en"].messages)
    es_keys = set(bundles["es"].messages)
    assert en_keys == es_keys, f"key mismatch: {en_keys ^ es_keys}"


def test_render_interpolates_params():
    bundles = load_bundles()
    text = bundles["en"].render("route-status-shipped.0", {"order_id": 123456, "carrier": "UPS"})
    assert "123456" in text and "UPS" in text


def test_render_leaves_unknown_placeholder():
    bundles = load_bundles()
    # missing order_id -> placeholder preserved, no crash
    text = bundles["en"].render("route-status-shipped.0", {})
    assert "{order_id}" in text


@pytest.mark.parametrize(
    "text,expected",
    [
        ("where is my order", "en"),
        ("please cancel my order", "en"),
        ("dónde está mi pedido", "es"),
        ("quiero cancelar mi pedido", "es"),
        ("hola, necesito ayuda", "es"),
        ("", "en"),
    ],
)
def test_detect_locale(text, expected):
    assert detect_locale(text, ("en", "es"), "en") == expected
