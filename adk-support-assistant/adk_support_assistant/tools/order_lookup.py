"""``fetchOrderStatus`` webhook tool with a pluggable order-lookup client.

Provides:
  * :class:`OrderLookupClient` - the interface,
  * :class:`MockOrderLookupClient` - in-memory impl for tests/dev,
  * :class:`HttpOrderLookupClient` - HTTP impl calling a configurable endpoint,
  * :func:`map_backend_status` - normalises a raw backend status to a flow status,
  * :func:`fetch_order_status` - the ADK ``FunctionTool`` entry point.

Flow-level statuses (as used in transition conditions): ``SHIPPED``,
``PENDING``, ``NOT_FOUND``.
"""

from __future__ import annotations

import abc
from dataclasses import dataclass
from typing import Protocol

import httpx

from ..config import OrderLookupConfig, get_settings

# Flow-level statuses referenced by transition conditions.
STATUS_SHIPPED = "SHIPPED"
STATUS_PENDING = "PENDING"
STATUS_NOT_FOUND = "NOT_FOUND"

# ---------------------------------------------------------------------------
# Backend status -> flow status normalisation.
#
# The flow only distinguishes SHIPPED vs PENDING (plus NOT_FOUND). Backends model
# many in-flight states; they all normalise to PENDING, while any shipped/
# fulfilled/delivered signal normalises to SHIPPED and a cancelled order to
# NOT_FOUND. Endpoints that already return one of the flow statuses pass through
# unchanged. Extend this map (or supply your own) for other backends.
# ---------------------------------------------------------------------------
_BACKEND_STATUS_MAP: dict[str, str] = {
    "SHIPPED": STATUS_SHIPPED,
    "FULFILLED": STATUS_SHIPPED,
    "DELIVERED": STATUS_SHIPPED,
    "PENDING": STATUS_PENDING,
    "SUBMITTED": STATUS_PENDING,
    "IN_PROCESS": STATUS_PENDING,
    "PROCESSING": STATUS_PENDING,
    "PACKING": STATUS_PENDING,
    "CANCELLED": STATUS_NOT_FOUND,
    "CANCELED": STATUS_NOT_FOUND,
}


def map_backend_status(raw_status: str | None) -> str:
    """Normalise a raw backend order-status string to a flow status."""
    if not raw_status:
        return STATUS_NOT_FOUND
    return _BACKEND_STATUS_MAP.get(raw_status.strip().upper(), STATUS_PENDING)


@dataclass(frozen=True)
class OrderStatusResult:
    order_id: str
    status: str  # SHIPPED / PENDING / NOT_FOUND
    found: bool
    raw_status: str | None = None
    carrier: str | None = None
    error: str | None = None

    def as_dict(self) -> dict[str, object]:
        return {
            "order_id": self.order_id,
            "status": self.status,
            "found": self.found,
            "raw_status": self.raw_status,
            "carrier": self.carrier,
            "error": self.error,
        }


class OrderLookupClient(Protocol):
    """Interface every order-lookup backend must implement."""

    def lookup(self, order_id: str) -> OrderStatusResult:  # pragma: no cover - protocol
        ...


class _BaseClient(abc.ABC):
    @abc.abstractmethod
    def lookup(self, order_id: str) -> OrderStatusResult:
        raise NotImplementedError


class MockOrderLookupClient(_BaseClient):
    """In-memory client. Maps order_id -> raw backend status for tests/dev."""

    #: Deterministic seed data used by tests and local runs.
    DEFAULT_DATA: dict[str, str] = {
        "123456": "SHIPPED",
        "654321": "SUBMITTED",  # -> PENDING
        "222222": "IN_PROCESS",  # -> PENDING
        "999999": "SHIPPED",
        "100000": "SUBMITTED",  # -> PENDING
    }

    #: Carrier reported for shipped orders (kept simple for tests/dev).
    DEFAULT_CARRIER = "UPS"

    def __init__(self, data: dict[str, str] | None = None):
        self._data = dict(self.DEFAULT_DATA if data is None else data)

    def set_status(self, order_id: str, raw_status: str) -> None:
        self._data[str(order_id)] = raw_status

    def lookup(self, order_id: str) -> OrderStatusResult:
        raw = self._data.get(str(order_id))
        if raw is None:
            return OrderStatusResult(order_id, STATUS_NOT_FOUND, found=False)
        status = map_backend_status(raw)
        carrier = self.DEFAULT_CARRIER if status == STATUS_SHIPPED else None
        return OrderStatusResult(
            order_id, status, found=True, raw_status=raw, carrier=carrier
        )


class HttpOrderLookupClient(_BaseClient):
    """Calls a configurable order-lookup HTTP endpoint.

    Expected JSON response, e.g.::

        {"orderNumber": "123456", "status": "SUBMITTED",
         "found": true, "carrier": "UPS"}

    ``status`` is a raw backend status (or already a flow status); it is
    normalised through :func:`map_backend_status`. ``carrier`` is optional.
    """

    def __init__(self, config: OrderLookupConfig | None = None, client: httpx.Client | None = None):
        self._config = config or get_settings().order_lookup
        self._client = client

    def _url(self, order_id: str) -> str:
        path = self._config.path.format(order_id=order_id)
        return f"{self._config.base_url.rstrip('/')}/{path.lstrip('/')}"

    def lookup(self, order_id: str) -> OrderStatusResult:
        headers = {}
        if self._config.api_key:
            headers["Authorization"] = f"Bearer {self._config.api_key}"
        url = self._url(order_id)
        try:
            if self._client is not None:
                resp = self._client.get(url, headers=headers, timeout=self._config.timeout_seconds)
            else:
                resp = httpx.get(url, headers=headers, timeout=self._config.timeout_seconds)
        except httpx.HTTPError as exc:
            return OrderStatusResult(
                order_id, STATUS_NOT_FOUND, found=False, error=f"transport: {exc}"
            )
        if resp.status_code == 404:
            return OrderStatusResult(order_id, STATUS_NOT_FOUND, found=False)
        if resp.status_code >= 400:
            return OrderStatusResult(
                order_id, STATUS_NOT_FOUND, found=False, error=f"http {resp.status_code}"
            )
        try:
            payload = resp.json()
        except ValueError as exc:
            return OrderStatusResult(
                order_id, STATUS_NOT_FOUND, found=False, error=f"decode: {exc}"
            )
        found = bool(payload.get("found", True))
        raw = payload.get("status")
        if not found:
            return OrderStatusResult(order_id, STATUS_NOT_FOUND, found=False, raw_status=raw)
        return OrderStatusResult(
            order_id,
            map_backend_status(raw),
            found=True,
            raw_status=raw,
            carrier=payload.get("carrier"),
        )


def build_client(config: OrderLookupConfig | None = None) -> OrderLookupClient:
    """Factory selecting the client implementation from configuration."""
    cfg = config or get_settings().order_lookup
    if cfg.client == "http":
        return HttpOrderLookupClient(cfg)
    return MockOrderLookupClient()


# Process-wide default client; overridable in tests via set_default_client().
_default_client: OrderLookupClient | None = None


def set_default_client(client: OrderLookupClient | None) -> None:
    global _default_client
    _default_client = client


def get_default_client() -> OrderLookupClient:
    global _default_client
    if _default_client is None:
        _default_client = build_client()
    return _default_client


def fetch_order_status(order_id: str) -> dict[str, object]:
    """Look up the status of an order by its 6-digit order number.

    This is the ADK tool implementation for the ``fetchOrderStatus`` webhook
    tag. Returns a dict with keys ``order_id``, ``status`` (SHIPPED / PENDING /
    NOT_FOUND), ``found``, ``raw_status`` and ``error``.

    Args:
        order_id: The customer's order number (6 digits, 100000-999999).
    """
    client = get_default_client()
    result = client.lookup(str(order_id))
    return result.as_dict()
