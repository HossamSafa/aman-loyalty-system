# loyalty-service – Admin Account Operations

**Service:** loyalty-service
**Base path:** `/api/v1/loyalty`
 

---

## 1. Purpose

Two related admin capabilities on top of a customer's loyalty account:

- **Freeze / Unfreeze (Flow 9):** Let a fraud analyst stop all loyalty mutations on a suspicious
  account and later reactivate it after review, with a full audit trail.
- **Manual Adjustment (Flow 8):** Let an operations user manually credit or debit a customer's
  points (e.g. compensation, correction) with a mandatory reason code and a full audit trail,
  without a real purchase or redemption.
---

## 2. Scope

| Method | Path | Flow | Description |
|---|---|---|---|
| POST | `/admin/accounts/{accountId}/freeze` | 9 | Freeze a loyalty account |
| POST | `/admin/accounts/{accountId}/unfreeze` | 9 | Reactivate a frozen account |
| POST | `/admin/accounts/{accountId}/adjustments` | 8 | Manually credit or debit an account |
 
---

## 3. Package structure

```
com.aman.acceptance.loyalty
├── controller
│   └── AdminAccountController.java          (freeze, unfreeze, and adjustments endpoints)
├── enums
│   ├── ErrorCode.java                       (LOYALTY_ACCOUNT_NOT_FOUND, LOYALTY_ACCOUNT_FROZEN,
│   │                                          LOYALTY_ACCOUNT_ALREADY_FROZEN, LOYALTY_ACCOUNT_NOT_FROZEN,
│   │                                          LOYALTY_INSUFFICIENT_AVAILABLE_POINTS, ...)
│   └── AdjustmentType.java                  (CREDIT, DEBIT)
├── exception
│   ├── LoyaltyException.java                (factory methods: notFound/conflict/locked/
│   │                                          badRequest/internal/unprocessable)
│   └── GlobalExceptionHandler.java          (@RestControllerAdvice)
├── model
│   ├── request
│   │   ├── FreezeAccountRequest.java
│   │   ├── UnfreezeAccountRequest.java
│   │   └── AdjustmentRequest.java
│   └── response
│       ├── ApiResponse.java                 (unified success/error envelope)
│       ├── ErrorDetails.java
│       ├── Meta.java
│       ├── AccountStatusResponse.java
│       └── AdjustmentResponse.java          
├── repository
│   ├── LoyaltyAccountRepository.java
│   ├── AuditEventRepository.java
│   ├── LoyaltyTransactionRepository.java
│   └── PointsLotRepository.java
└── service
    ├── AccountFreezeService.java            
    └── AdjustmentService.java               
```
 
---

## 4. API Contract

### 4.1 Freeze an account — `POST /admin/accounts/{accountId}/freeze`

Request:
```json
{
  "reasonCode": "SUSPICIOUS_REDEMPTION_PATTERN",
  "note": "Multiple OTP failures across terminals",
  "actorId": "fraud-analyst-01"
}
```

Success — `200 OK`:
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

### 4.2 Unfreeze an account — `POST /admin/accounts/{accountId}/unfreeze`

Request:
```json
{
  "reasonCode": "REVIEW_COMPLETED",
  "note": "Customer identity validated by operations",
  "actorId": "fraud-analyst-01"
}
```

Response shape identical to Freeze, with `"status": "ACTIVE"`.

### 4.3 Manual adjustment — `POST /admin/accounts/{accountId}/adjustments`

Request:
```json
{
  "type": "CREDIT",
  "points": 500,
  "reasonCode": "SERVICE_RECOVERY",
  "note": "Compensation approved by operations case CS-7781",
  "expiresInDays": 360,
  "actorId": "ops-user-01"
}
```

Success — `201 Created`:
```json
{
  "success": true,
  "data": {
    "adjustmentId": 3,
    "loyaltyTransactionId": 3,
    "type": "CREDIT",
    "points": 500,
    "balance": {
      "available": 3000,
      "locked": 1000,
      "reserved": 0,
      "totalOwned": 4000
    },
    "auditId": 7
  },
  "meta": {
    "correlationId": "cor-06387e61",
    "timestamp": "2026-08-17T13:35:20.748336200Z"
  }
}
```



### 4.4 Error responses

| HTTP | Error code | When | Flow |
|---|---|---|---|
| 400 | `LOYALTY_VALIDATION_ERROR` | `reasonCode`/`actorId` missing/blank (freeze, unfreeze); `type`, `points` (must be positive), `reasonCode`, or `actorId` missing/invalid (adjustment) | 8 & 9 |
| 404 | `LOYALTY_ACCOUNT_NOT_FOUND` | `accountId` does not exist | 8 & 9 |
| 409 | `LOYALTY_ACCOUNT_ALREADY_FROZEN` | Freeze called on an account already `FROZEN` | 9 |
| 409 | `LOYALTY_ACCOUNT_NOT_FROZEN` | Unfreeze called on an account that is not `FROZEN` | 9 |
| 422 | `LOYALTY_INSUFFICIENT_AVAILABLE_POINTS` | Debit requested exceeds `availablePoints` | 8 |
| 423 | `LOYALTY_ACCOUNT_FROZEN` | Account is frozen — triggered when Earn/Redeem/Adjustment call `assertAccountActive()` | 8 & 9 |
| 500 | `LOYALTY_INTERNAL_ERROR` | Any unexpected error (shared fallback handler) | 8 & 9 |

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
 
---

## 5. Business rules implemented

**Freeze / Unfreeze (Flow 9)**
- An account can only be frozen from `ACTIVE` state, and unfrozen only from `FROZEN` state.
  Attempting either in the wrong state returns `409 Conflict` rather than silently succeeding —
  keeps the audit trail meaningful and surfaces operator mistakes instead of hiding them.
- Every freeze/unfreeze call writes an immutable `AuditEvent` row with a before/after JSON snapshot
  of the account (`accountId`, `status`, `availablePoints`, `lockedPoints`, `reservedPoints`), plus
  `actorId`, `action`, `entityType`, `entityId`, and `correlationId`.
- The state change and the audit write happen inside a single `@Transactional` boundary — if either
  fails, both roll back.
- `assertAccountActive(LoyaltyAccount account)` is exposed as a public method on
  `AccountFreezeService` for other flows to call before any point mutation. It throws
  `LoyaltyException.locked(ErrorCode.LOYALTY_ACCOUNT_FROZEN, ...)` (HTTP 423) when frozen.

**Manual Adjustment (Flow 8)**
- Credit creates a new `PointsLot` with status `AVAILABLE` immediately (no 30-day lock), because a
  manual credit has no associated purchase to define a refund window.
- Debit validates `availablePoints >= requestedPoints` first, then consumes `AVAILABLE` lots FIFO by
  nearest expiry,
  decrementing `remainingPoints` lot by lot.
- Every adjustment writes a `LoyaltyTransaction` row (`ADJUSTMENT_CREDIT` / `ADJUSTMENT_DEBIT`) as the
  immutable ledger entry, plus an `AuditEvent` with a before/after account snapshot.
- Before any mutation, the account is checked via `AccountFreezeService.assertAccountActive(account)` —
  the exact guard method built in Flow 9. A frozen account gets `423 LOYALTY_ACCOUNT_FROZEN` and nothing
  is written. This flow does not duplicate that check.
- The whole operation (transaction write, lot mutation, account balance update, audit write) happens
  inside one `@Transactional` boundary.

---

## 6. How to run locally

1. Ensure PostgreSQL is running and the `aman_loyalty` database exists.
2. `application.yml` already has `defer-datasource-initialization: true` and
   `spring.sql.init.mode: always` so `data.sql` seeds:
    - one test account (`accountId = 1`, `ACTIVE`, `available=2500`, `locked=1000`)
    - one `EARN` transaction and one `AVAILABLE` points lot (`accountId = 1`, 2500 points) so debit
      scenarios in Flow 8 have real lots to consume
3. Run the app: `mvn spring-boot:run` (or run `Application.java` from the IDE).
4. App available at `http://localhost:8080/api/v1/loyalty`.
5. Swagger UI: `http://localhost:8080/api/v1/loyalty/swagger-ui/index.html`.
---

## 7. Testing

**Postman collection:** `Loyalty_Admin_Operations.postman_collection.json`

- **Freeze / Unfreeze**
    - Freeze — success (200)
    - Freeze — already frozen (409)
    - Unfreeze — success (200)
    - Unfreeze — not frozen (409)
    - Freeze — account not found (404)
    - Freeze — missing reasonCode (400)
    - Freeze — missing actorId (400)
- **Adjustments**
    - Credit — success (201)
    - Debit — success (201)
    - Debit — insufficient points (422)
    - Adjustment on a frozen account (423) 
    - Account not found (404)
    - Validation — negative points (400)

**Unit tests (Mockito):**
- `AccountFreezeServiceTest` — covers the freeze/unfreeze scenarios above at the service layer.
- `AdjustmentServiceTest` — covers the adjustment scenarios above at the service layer.