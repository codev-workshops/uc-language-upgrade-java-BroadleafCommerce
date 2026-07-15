# ADK Support Assistant

A standalone, multilingual (**en** / **es**), text **and** voice customer-support
assistant built on **Google ADK** (`google-adk`) driven by an **in-house LLM**
(via `litellm` / `LiteLlm`) instead of Google Conversational Agents.

It replicates the Dialogflow CX **`OrderManagementFlow`** (pages `WelcomePage`,
`OrderLookupPage`, `ModifyOrderPage`; a required 6-digit `order_id` slot; intent
and condition transition routes; `sys.no-match` escalation to a live agent; and
the `ERP_Lookup_Service` webhook with tag `fetchOrderStatus`) using a
**data-driven, deterministic flow engine**. The Dialogflow CX export is parsed
from config — no flow logic is hard-coded — so new flows and languages are added
without touching engine code.

> This app lives in its own top-level directory and does **not** modify the
> Broadleaf Commerce Java code. A reference Spring controller backing the order
> lookup with `OrderService#findOrderByOrderNumber` is documented below (opt-in).

## Architecture

```
user text/audio ─► channel gateway (REST/WebSocket)
                      │            ▲
                 STT adapter   TTS adapter        (voice/, locale-aware, swappable)
                      │            │
                      ▼            │
              ┌──────────────── FlowEngine ────────────────┐   (flow/engine.py)
              │  language detect → NLU → slot fill →        │
              │  safe condition eval → route → webhook      │
              └──┬───────────┬──────────────┬───────────────┘
                 │           │              │
          NLU (LLM/rules)  i18n bundles   fetchOrderStatus tool
          nlu/             i18n/          tools/order_lookup.py
                 │                          │
          in-house LLM (litellm)      Mock / HTTP → Broadleaf endpoint
          llm/in_house_llm.py
```

Every turn is traced (input, intent+entities, chosen route, tool calls,
latencies) with **PII redaction** for order IDs and names (`observability/`).

The same engine is exposed three ways:
* **`adk run` / `adk web`** via `adk_support_assistant.agent:root_agent`
  (a `FlowAgent(BaseAgent)` that bridges ADK's turn loop to the engine),
* **REST + WebSocket** via the FastAPI channel gateway, and
* **directly** as a Python object (`build_engine()`).

## Layout

| Path | What |
|------|------|
| `flows/order_management_flow.json` | Dialogflow-CX-style flow export (source of truth) |
| `i18n/en.json`, `i18n/es.json` | Per-locale copy keyed by fulfillment id |
| `adk_support_assistant/flow/` | Pydantic schema, CX loader, safe condition evaluator, engine |
| `adk_support_assistant/nlu/` | Intent/entity classification (LLM + rule-based) with strict-JSON output |
| `adk_support_assistant/llm/` | In-house OpenAI-compatible LLM adapter (`LiteLlm`) |
| `adk_support_assistant/tools/` | `fetchOrderStatus` webhook tool + live-agent handoff |
| `adk_support_assistant/voice/` | Swappable STT / TTS adapters |
| `adk_support_assistant/channel/` | FastAPI REST + WebSocket gateway |
| `adk_support_assistant/observability/` | Per-turn tracing + PII redaction |
| `adk_support_assistant/agent.py` | `root_agent` for `adk run` / `adk web` |
| `eval/*.test.json` | Full-trajectory conversation evaluation files |
| `tests/` | Unit + integration + golden-dataset + trajectory tests |

## Quickstart

```bash
cd adk-support-assistant
python -m venv .venv && . .venv/bin/activate
pip install -e ".[dev]"
cp .env.example .env        # then edit for your in-house LLM + backend

# run the test suite (offline, deterministic)
pytest -q

# run the REST/WebSocket gateway
adk-support-gateway         # or: uvicorn adk_support_assistant.channel.gateway:create_app --factory

# run through ADK
adk run adk_support_assistant
adk web                     # then pick "adk_support_assistant"
```

### Text REST example

```bash
curl -XPOST localhost:8000/session -H 'content-type: application/json' -d '{"locale":"en"}'
curl -XPOST localhost:8000/session/s-1/text -H 'content-type: application/json' \
     -d '{"text":"where is my order"}'
curl -XPOST localhost:8000/session/s-1/text -H 'content-type: application/json' \
     -d '{"text":"123456"}'
```

## Configuring the in-house LLM endpoint

The model is reached through LiteLLM's OpenAI-compatible provider. Set in `.env`:

```
INHOUSE_LLM_MODEL=openai/<your-model>      # "openai/" routes to an OpenAI-compatible server
INHOUSE_LLM_API_BASE=https://llm.internal.example.com/v1
INHOUSE_LLM_API_KEY=...
INHOUSE_LLM_TEMPERATURE=0.0
INHOUSE_LLM_MAX_TOKENS=512
INHOUSE_LLM_TIMEOUT_SECONDS=30
```

`llm/in_house_llm.py` builds both the ADK `LiteLlm` model (for agents) and a
`complete_json()` helper used by the NLU classifier, which requests structured
output (`response_format` json / json_schema) and validates it against
`NluResult`. If the LLM is unavailable or returns invalid JSON, NLU falls back
to the deterministic rule-based classifier so the flow keeps working.

To implement a fully custom `BaseLlm` instead of LiteLLM, subclass
`google.adk.models.base_llm.BaseLlm` and return it from `build_in_house_llm()`.

## Adding a new language

1. Copy `i18n/en.json` to `i18n/<locale>.json` and translate the values.
   **Keys (fulfillment ids) must match exactly** across locales — a test
   enforces parity.
2. Add the locale to `SUPPORTED_LOCALES` in `.env`.
3. Extend detection: add markers to `_INTENT_KEYWORDS` (per-locale) in
   `nlu/classifier.py` and language markers in `i18n/bundle.py::detect_locale`
   (or swap in a model-based detector behind the same signature).
4. Add a golden dataset `tests/data/nlu_golden_<locale>.json` and a trajectory
   `eval/<name>.test.json` for the new locale.

Flow structure is language-independent, so no engine or flow-JSON changes are
needed.

## Adding a new flow via config

1. Export your Dialogflow CX flow (or author JSON in the same shape) and drop it
   in `flows/`. The loader accepts both camelCase (CX API export) and snake_case.
2. Add fulfillment ids used by the flow to every `i18n/<locale>.json`.
3. Point `flow_file` at it (env or `Settings`) and, if it calls a webhook,
   register a client for its tag (see `tools/order_lookup.py` as the template).
4. Add trajectory `.test.json` files under `eval/`.

Supported flow features: entry/trigger fulfillments, forms with required
parameters + validation, initial prompt & reprompt handlers, intent- and
condition-based transition routes, `setParameterActions`, event handlers
(`sys.no-match-1/2`, `sys.invalid-param`), `liveAgentHandoff`, and webhooks.

### Safe condition evaluation

Transition/validation conditions (e.g.
`$session.params.order_id >= 100000 AND $session.params.order_id <= 999999`) are
parsed by a **restricted recursive-descent evaluator** (`flow/conditions.py`) —
**no `eval`/`exec`** — so malicious flow content or reflected user input cannot
execute code. Supported: references (`$session.params.*`, `$page.params.*`,
`$flow.params.*`, `$intent`), comparisons (`= == != < <= > >=`), `AND/OR/NOT`,
parentheses, and truthiness of a bare reference.

## Order lookup & the Broadleaf backend

`tools/order_lookup.py` defines the `OrderLookupClient` interface with two
implementations selected via `ORDER_LOOKUP_CLIENT`:

* `mock` — in-memory, seeded data (used by tests/dev),
* `http` — calls a Broadleaf order-lookup endpoint at
  `ORDER_LOOKUP_BASE_URL + ORDER_LOOKUP_PATH` with a timeout; any transport
  error / non-2xx / bad JSON → graceful fallback (the flow escalates to a live
  agent via `system.error.lookup_failed`).

Broadleaf `OrderStatus` is mapped to the flow's statuses by
`map_broadleaf_status()`:

| Broadleaf `OrderStatus` | Flow status |
|-------------------------|-------------|
| `SHIPPED` / `FULFILLED` (synthetic, from fulfillment) | `SHIPPED` |
| `SUBMITTED`, `IN_PROCESS`, `NAMED`, `QUOTE`, `CSR_OWNED` | `PENDING` |
| `CANCELLED` | `NOT_FOUND` |
| unknown / missing | `PENDING` / `NOT_FOUND` |

### Reference backend endpoint (opt-in, not wired into the Java build)

To keep the existing Broadleaf code untouched, the endpoint is **not** added to
the Maven modules. If you want to expose it, add a controller like the following
to `core/broadleaf-framework-web` (it uses the existing
`OrderService#findOrderByOrderNumber(String)` and
`org.broadleafcommerce.core.order.service.type.OrderStatus`) and set
`ORDER_LOOKUP_CLIENT=http`:

```java
@RestController
@RequestMapping("/api/support/orders")
public class SupportOrderLookupController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @GetMapping("/{orderNumber}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        Map<String, Object> body = new HashMap<>();
        body.put("orderNumber", orderNumber);
        if (order == null) {
            body.put("found", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        OrderStatus status = order.getStatus();
        body.put("found", true);
        // Prefer a shipped signal derived from fulfillment status when available;
        // otherwise pass the raw OrderStatus type through (mapped client-side).
        body.put("status", status == null ? null : status.getType());
        return ResponseEntity.ok(body);
    }
}
```

The Python `HttpOrderLookupClient` maps that `status` string through
`map_broadleaf_status()`.

## Tests & CI

`pytest -q` runs entirely offline (rule-based NLU + in-memory order client):

* `test_conditions.py` — safe evaluator (incl. code-injection rejection)
* `test_loader_schema.py` — CX loader / schema
* `test_order_lookup.py` — `order_id` validation + `fetchOrderStatus` (mock & HTTP)
* `test_nlu_golden.py` — per-language golden datasets with an accuracy threshold
* `test_flow_engine.py` — welcome→invalid→reprompt→valid→lookup→SHIPPED/PENDING
* `test_handoff.py` — no-match escalation to live-agent handoff
* `test_multilingual_parity.py` — en vs es reach the same pages/ids, localized text
* `test_voice_integration.py` — STT→flow and flow→TTS
* `test_gateway.py` — REST + WebSocket + audio
* `test_adk_agent.py` — full trajectory through a real ADK `InMemoryRunner`
* `test_trajectories.py` — replays `eval/*.test.json` full conversations
* `test_observability.py` — PII redaction + tracing

CI is wired in `.github/workflows/adk-support-assistant.yml` (lint + tests on
3.10–3.12). The `.env` is never required for tests.
