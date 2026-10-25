# Changelog

All notable changes to the fake-payment-gateway are documented here.

## [Unreleased]

### Fixed — L-01 webhook retries blocked the request thread

- `retryAsync` was a `private` method called from the same class, so `@Async` never applied and retries (2 s + 10 s backoff) ran inside the HTTP request and its transaction.
- Split `WebhookNotifierAdapter` into `WebhookDeliveryClient` (one signed attempt + audit log, never throws) and `WebhookRetryScheduler` (separate bean, public `@Async` method). First attempt stays synchronous; retries run in the async pool.
- Backoff is configurable with `WEBHOOK_RETRY_BACKOFF_MS` (default `2000,10000`, 3 attempts total).
- 4xx/5xx responses from the receiver are now logged with their HTTP status (before, they were recorded as network errors with a null status).
- L-04: outgoing webhook `RestTemplate` now has connect/read timeouts (`WEBHOOK_CONNECT_TIMEOUT_MS`=3000, `WEBHOOK_READ_TIMEOUT_MS`=5000).

### Fixed — L-02 client errors returned 500

- `GlobalExceptionHandler` now maps validation errors to 400 `Validation failed` with per-field messages, malformed JSON / invalid enum to 400 `Malformed request`, plus 404, 405, 409 and 415. Unhandled errors return 500 with a generic message and are logged with their stack trace.
- L-12: `currency` must be 3 letters and `externalReference` at most 100 characters (matches the DB columns).

### Security — L-03 `/admin/**` had no authentication

- New `AdminApiKeyInterceptor` (Spring MVC interceptor on `/admin/**`, evaluated on the same normalized path used for routing, so `/%61dmin/...` and `/admin;x=1/...` cannot bypass it): `/admin/**` requires `X-Api-Key` (constant-time comparison, `ADMIN_API_KEY`). CORS preflight is exempt. `ADMIN_API_KEY` has no default (fail-closed): startup fails if it is missing, blank or shorter than 16 characters, and warns if the development key is used.
- New public checkout endpoints `POST /public/checkout/{id}/{approve|decline|fail|cancel}`; `pay.html` uses them instead of `/admin`. Refund is no longer reachable from the checkout (merchant-only).
- TuEvento backend: `FakePaymentGatewayAdapter` sends `X-Api-Key` on cancel/refund (`payment.fake-gateway.api-key` / `FAKE_GATEWAY_API_KEY`). `docker-compose.yml` passes the same value to the gateway.

### Added — L-09 automated tests

- 53 tests across domain, use cases, webhook signing (RFC 4231 vector), delivery, retries (including a real `@Async` proxy test), the API-key filter and the HTTP contract (`@WebMvcTest`). Run with `./mvnw test`; no database required.

### Docs

- Product documentation set under `docs/` (vision, SRS, architecture, API, webhooks, data model, integration, deployment, test plan, security, roadmap, glossary, ADR-001…006).

### Added — feat(payment-refund)

- **Domain — PaymentStatus**: Added `REFUNDED` state with valid transition `APPROVED → REFUNDED` (all other origin states throw `InvalidStatusTransitionException`). `REFUNDED` is terminal.
- **Domain — Payment**: Added `refund()` method — transitions to `REFUNDED` and returns a `PaymentStatusChanged` event (same pattern as `approve()`, `decline()`, `fail()`, `cancel()`).
- **Application — RefundPaymentUseCase**: New `@Service` that loads the payment, calls `payment.refund()` (domain validates the transition), persists, and fires the webhook via the existing `WebhookNotifierPort` — no new notification mechanism introduced.
- **Webhook — payment.refunded event**: Reuses `WebhookNotifierAdapter` unchanged. Because the payload is built as `"payment." + status.name().toLowerCase()`, the new `REFUNDED` status automatically produces the `"payment.refunded"` event type with the same contract (`eventId`, `event`, `paymentId`, `status`, `amount`, `currency`, `timestamp`), HMAC-SHA256 signature, and retry/audit logic.
- **Interfaces — AdminPaymentController**: Added `POST /admin/payments/{id}/refund` endpoint, wired to `RefundPaymentUseCase`, symmetric to existing approve/decline/fail/cancel endpoints.
- **UI — pay.html**: Added `↩️ Reembolsar` button visible **only** when payment status is `APPROVED`. Added `REFUNDED` status badge style (purple), `renderTerminal` message for `REFUNDED`, updated `isTerminal()` to include `REFUNDED`, and `actionLabel` mapping.

### Fixed

- **pom.xml**: Added `maven-compiler-plugin` `annotationProcessorPaths` entry for Lombok — fixes pre-existing compilation failure where `@Getter`/`@Setter` on `PaymentEntity` and `WebhookDeliveryLogEntity` were not being processed, causing `cannot find symbol` errors for all generated accessors.
