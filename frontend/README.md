# Pratos Orchestration Payment Platform — Frontend

React + TypeScript + Vite admin UI for the Spring Boot payment orchestration backend.

## Screens
- Admin login using backend JWT
- Overview dashboard with transaction metrics and illustrative activity chart
- Transactions list and create-transaction form
- Provider/gateway management
- Provider credentials form (MID, environment, reference ID, merchant/salt keys)
- Payment attempts with start/success/failure test actions
- Smart routing explanation
- Reports and reconciliation readiness summary

## Requirements
- Node.js 20+ LTS
- Spring Boot backend running at http://localhost:8080
- Existing backend admin account

## Run on Windows
1. Extract/copy this folder to `E:\PratosOrchestrationPaymentPlatform\frontend`.
2. Open PowerShell or IntelliJ terminal in that folder.
3. Run:
   ```powershell
   npm install
   Copy-Item .env.example .env
   npm run dev
   ```
4. Open `http://localhost:5173` and sign in with your backend admin credentials.

## Backend routes used
- `POST /api/v1/auth/login`
- `GET/POST /api/v1/admin/payment-transactions`
- `GET/POST /api/v1/admin/credentials`
- `GET /api/v1/admin/providers`
- `GET /api/v1/admin/payment-attempts`
- `PATCH /api/v1/admin/payment-attempts/{id}/start`
- `POST /api/v1/admin/payment-attempts/{id}/success`
- `POST /api/v1/admin/payment-attempts/{id}/failure`

If your controller route differs, edit `src/api.ts`. Provider and report list endpoints are optional and show empty/fallback UI if unavailable. Attempt actions are intended for sandbox testing only; this frontend does not execute real payments.
