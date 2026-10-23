# Changelog

All notable changes to the fake-payment-gateway are documented here.

## [Unreleased]

### Added — feat(payment-refund)

- **Domain — PaymentStatus**: Added `REFUNDED` state with valid transition `APPROVED → REFUNDED` (all other origin states throw `InvalidStatusTransitionException`). `REFUNDED` is terminal.
- **Domain — Payment**: Added `refund()` method — transitions to `REFUNDED` and returns a `PaymentStatusChanged` event (same pattern as `approve()`, `decline()`, `fail()`, `cancel()`).
- **Application — RefundPaymentUseCase**: New `@Service` that loads the payment, calls `payment.refund()` (domain validates the transition), persists, and fires the webhook via the existing `WebhookNotifierPort` — no new notification mechanism introduced.
- **Webhook — payment.refunded event**: Reuses `WebhookNotifierAdapter` unchanged. Because the payload is built as `"payment." + status.name().toLowerCase()`, the new `REFUNDED` status automatically produces the `"payment.refunded"` event type with the same contract (`eventId`, `event`, `paymentId`, `status`, `amount`, `currency`, `timestamp`), HMAC-SHA256 signature, and retry/audit logic.
- **Interfaces — AdminPaymentController**: Added `POST /admin/payments/{id}/refund` endpoint, wired to `RefundPaymentUseCase`, symmetric to existing approve/decline/fail/cancel endpoints.
- **UI — pay.html**: Added `↩️ Reembolsar` button visible **only** when payment status is `APPROVED`. Added `REFUNDED` status badge style (purple), `renderTerminal` message for `REFUNDED`, updated `isTerminal()` to include `REFUNDED`, and `actionLabel` mapping.

### Fixed

- **pom.xml**: Added `maven-compiler-plugin` `annotationProcessorPaths` entry for Lombok — fixes pre-existing compilation failure where `@Getter`/`@Setter` on `PaymentEntity` and `WebhookDeliveryLogEntity` were not being processed, causing `cannot find symbol` errors for all generated accessors.
