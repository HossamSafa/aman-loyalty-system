
# Flow 9 – Fraud Freeze & Unfreeze

**Service:** `loyalty-service`
**Branch:** `feature/flow-9-freeze-unfreeze-account`
**Base path:** `/api/v1/loyalty`
 

---

## 1. Purpose

Allow a fraud analyst to stop all loyalty mutations on a suspicious account, cancel any
active point reservations, and preserve a full audit trail. The account can later be
reactivated (unfrozen) after review, with the audit history intact.

## 2. Scope of this branch

This branch implements **only** the two admin endpoints below and the reusable guard
that other flows will call.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/admin/accounts/{accountId}/freeze` | Freeze a loyalty account |
| `POST` | `/admin/accounts/{accountId}/unfreeze` | Reactivate a frozen account |

## 3. Package structure added

```
com.aman.acceptance.loyalty
├── controller
│   └── AdminAccountController.java
├── enums
│   └── ErrorCode.java                       (LOYALTY_ACCOUNT_NOT_FOUND, LOYALTY_ACCOUNT_FROZEN, ...)
├── exception
│   ├── LoyaltyException.java                (single business exception, factory methods:
│   │                                          notFound/conflict/locked/badRequest/internal)
│   └── GlobalExceptionHandler.java          (@RestControllerAdvice)
├── model
│   ├── request
│   │   ├── FreezeAccountRequest.java
│   │   └── UnfreezeAccountRequest.java
│   └── response
│       ├── ApiResponse.java                 (unified success/error envelope)
│       ├── ErrorDetails.java
│       ├── Meta.java
│       └── AccountStatusResponse.java
├── repository
│   ├── LoyaltyAccountRepository.java
│   └── AuditEventRepository.java
└── service
    └── AccountFreezeService.java
```

## 4. API Contract

### 4.1 Freeze an account

```
POST /api/v1/loyalty/admin/accounts/{accountId}/freeze
Content-Type: application/json
```

Request body:
```json
{
  "reasonCode": "SUSPICIOUS_REDEMPTION_PATTERN",
  "note": "Multiple OTP failures across terminals",
  "actorId": "fraud-analyst-01"
}
```

Success response — `200 OK`:
```json
{
  "success": true,
  "data": {
    "accountId": 1,
    "status": "FROZEN",
    "changedAt": "2026-08-09T11:20:00",
    "auditId": 1
  },
  "meta": {
    "correlationId": "cor-5acbff68",
    "timestamp": "2026-08-09T11:20:00.774247700Z"
  }
}
```

### 4.2 Unfreeze an account

```
POST /api/v1/loyalty/admin/accounts/{accountId}/unfreeze
Content-Type: application/json
```

Request body:
```json
{
  "reasonCode": "REVIEW_COMPLETED",
  "note": "Customer identity validated by operations",
  "actorId": "fraud-analyst-01"
}
```

Response shape is identical to Freeze, with `"status": "ACTIVE"`.

### 4.3 Error responses

| HTTP | Error code | When |
|------|-----------|------|
| 400 | `LOYALTY_VALIDATION_ERROR` | `reasonCode` or `actorId` missing/blank |
| 404 | `LOYALTY_ACCOUNT_NOT_FOUND` | `accountId` does not exist |
| 409 | `LOYALTY_ACCOUNT_ALREADY_FROZEN` | Freeze called on an account already `FROZEN` |
| 409 | `LOYALTY_ACCOUNT_NOT_FROZEN` | Unfreeze called on an account that is not `FROZEN` |
| 423 | `LOYALTY_ACCOUNT_FROZEN` | Reserved for other flows (Earn/Redeem/Adjustment) when they call `assertAccountActive()` on a frozen account — **not triggered by this branch's own endpoints** |
| 500 | `LOYALTY_INTERNAL_ERROR` | Any unexpected error (fallback handler) |

Example error body:
```json
{
  "success": false,
  "error": {
    "code": "LOYALTY_ACCOUNT_ALREADY_FROZEN",
    "message": "Account 1 is already frozen.",
    "retryable": false
  },
  "meta": {
    "correlationId": "cor-7d8f2c",
    "timestamp": "2026-08-09T11:20:00Z"
  }
}
```

All response fields are guaranteed to serialize in the order
`success → data → error → meta` via `@JsonPropertyOrder`, matching the
reference design's response envelope exactly.

## 5. Business rules implemented

- An account can only be frozen from `ACTIVE` state, and unfrozen only from `FROZEN`
  state. Attempting either in the wrong state returns `409 Conflict` rather than
  silently succeeding — this keeps the audit trail meaningful and surfaces operator
  mistakes instead of hiding them.
- Every freeze/unfreeze call writes an immutable `AuditEvent` row with a before/after
  JSON snapshot of the account (`accountId`, `status`, `availablePoints`,
  `lockedPoints`, `reservedPoints`), plus `actorId`, `action`, `entityType`,
  `entityId`, and `correlationId`.
- The state change and the audit write happen inside a single `@Transactional`
  boundary — if either fails, both roll back.
- `assertAccountActive(LoyaltyAccount account)` is exposed as a public method on
  `AccountFreezeService` for other flows to call before any point mutation. It throws
  `LoyaltyException.locked(ErrorCode.LOYALTY_ACCOUNT_FROZEN, ...)` (HTTP 423) when
  the account is frozen.
## 7. How to run locally

1. Ensure PostgreSQL is running and the `aman_loyalty` database exists.
2. `application.yml` already has `defer-datasource-initialization: true` and
   `spring.sql.init.mode: always` so `data.sql` seeds one test account
   (`accountId = 1`, `ACTIVE`, `available=2500`, `locked=1000`) on every startup.
3. Run the app: `mvn spring-boot:run` (or run `Application.java` from the IDE).
4. App is available at `http://localhost:8080/api/v1/loyalty`.
5. Swagger UI: `http://localhost:8080/api/v1/loyalty/swagger-ui/index.html`.
## 8. Testing

A ready-to-import Postman collection is included:
`Flow9_Freeze_Unfreeze.postman_collection.json`

It covers:
1. Freeze — success (200)
2. Freeze — already frozen (409)
3. Unfreeze — success (200)
4. Unfreeze — not frozen (409)
5. Freeze — account not found (404)
6. Freeze — missing `reasonCode` (400)
7. Freeze — missing `actorId` (400)
 






