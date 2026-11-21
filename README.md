# Pratosh Payment Orchestration Platform

Spring Boot 4 + MySQL backend and a React/Vite admin console. An administrator
configures payment providers, credentials, payment methods and currencies; the
platform routes each payment to an eligible provider, records every attempt,
retries safely, reports on outcomes and reconciles against provider reports.

> **Payments are simulated.** `payment.gateway.mode=MOCK` uses
> `MockPaymentGateway`, which moves no money. A real provider needs its own
> `PaymentGateway` implementation (API calls, signature verification, status
> verification after timeouts).

## Prerequisites
Java 21, Maven (wrapper included), Node 20+, MySQL 8 with an empty database
`payflow_db` (`CREATE DATABASE payflow_db;`).

## Configuration
Secrets are never in source. Copy `.env.example` to `.env` (git-ignored, read
automatically from the project root) or set the same variables in the
environment:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection (no default password) |
| `JWT_SECRET` | Base64, >= 32 bytes: `openssl rand -base64 32` |
| `CREDENTIAL_ENCRYPTION_KEY` | Base64, exactly 32 bytes (AES-256-GCM): `openssl rand -base64 32` |
| `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | First admin, created only if the username does not exist (an existing admin keeps its old password) |
| `PAYMENT_ENVIRONMENT` | `SANDBOX` (default) or `PRODUCTION` |

Changing `CREDENTIAL_ENCRYPTION_KEY` makes previously stored secrets
undecryptable; rotate a credential's secrets with the endpoint below.

## Run
```
./mvnw spring-boot:run          # backend on :8080
cd frontend && npm install && npm run dev   # UI on :5173 (proxies /api)
./mvnw test                     # unit tests (the context test needs MySQL)
```

## API (all `/api/v1/admin/**` require an ADMIN JWT)
Login: `POST /api/v1/auth/login` `{username,password}` -> `accessToken`;
send `Authorization: Bearer <token>`.

| Area | Endpoints |
|---|---|
| Providers | `POST/GET /admin/providers`, `GET /{id}`, `/enabled`, `PATCH /{id}/status`, `PATCH /{id}/priority` |
| Credentials | `POST/GET /admin/credentials`, `GET /provider/{id}`, `GET /{id}`, `PATCH /{id}/status`, **`PUT /{id}/secrets`** (write-only rotation) |
| Payment methods / currencies | `/admin/providers/{id}/payment-methods`, `/admin/providers/{id}/currencies` (+ status PATCHes) |
| Transactions | `POST/GET /admin/payment-transactions` (filters `status, providerId, paymentMethod, from, to, page, size`; total in `X-Total-Count`), `GET /{reference}`; public `POST /payments` |
| Attempts | `POST/GET /admin/payment-attempts`, **`POST /{id}/process`** (run through the gateway), `PATCH /{id}/start`, `/success`, `/failure` |
| Reports | `GET /admin/reports/insights?days=7` (live breakdown by currency, provider, method, day, failure reason), `GET /admin/reports/summary`, `GET /admin/reports/transactions.csv` |
| Reconciliation | `POST /admin/reconciliation/run`, `GET /batches/{id}`, `GET /discrepancies`, `PATCH /records/{id}/review` |

Idempotency: send `Idempotency-Key: <unique>` when creating a transaction.
Repeating the request returns the original transaction; reusing the key with a
different body returns 422.

### Demo scenarios (mock gateway, by merchant order ID)
`...FAIL_NETWORK` retryable failure, `...FAIL_DECLINE` final failure,
`...FAIL_TIMEOUT` uncertain outcome (never auto-retried), otherwise success.

Example end-to-end: create provider -> add credential -> enable a payment
method and currency -> `POST /admin/payment-transactions` -> create attempt ->
`POST /admin/payment-attempts/{id}/process` -> `GET /admin/reports/summary`.

### Reconciliation run
```
POST /api/v1/admin/reconciliation/run
{"providerId":1,"from":"2026-10-01","to":"2026-10-31",
 "rows":[{"providerTransactionReference":"MOCK-...","status":"CAPTURED","amount":100.00,"currency":"INR"}]}
```
Results: MATCHED, STATUS_MISMATCH, AMOUNT_MISMATCH, MISSING_ON_PLATFORM,
MISSING_AT_PROVIDER. Records are kept; reviewing adds a note (audit trail).

## Behaviour notes
* Retries: only NETWORK_ERROR, PROVIDER_UNAVAILABLE and
  TEMPORARY_PROVIDER_ERROR are retried, up to `payment.retry.max-attempts`,
  on a different provider. TIMEOUT/UNKNOWN set the transaction to UNKNOWN and
  require verification. If no alternative provider exists the transaction is
  marked FAILED. A unique `(transaction, attempt_number)` constraint prevents
  duplicate attempts even with several instances.
* Routing: enabled provider + enabled payment method + enabled currency +
  enabled credential for the environment; lowest priority number wins.
* Error responses: the ERROR dispatch is permitted in `SecurityConfig` so real
  404/405/500 are no longer masked as 401.

## Known limitations
No real provider integration; no audit log for provider/credential changes;
no reconciliation or CSV screens in the UI yet; the scheduler is not
distributed-locked (the DB constraint guards correctness, not efficiency); no
HTTPS/CORS config (UI uses the Vite proxy; configure both before deploying).
