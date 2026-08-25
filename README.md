# loyalty-service – Admin Account Operations

**Service:** loyalty-service
**Base path:** `/api/v1/loyalty`

---

## 1. Purpose

Three related admin capabilities on top of a customer's loyalty data:

- **Freeze / Unfreeze (Flow 9):** Let a fraud analyst stop all loyalty mutations on a suspicious
  account and later reactivate it after review, with a full audit trail.
- **Manual Adjustment (Flow 8):** Let an operations user manually credit or debit a customer's
  points (e.g. compensation, correction) with a mandatory reason code and a full audit trail,
  without a real purchase or redemption.
- **Customer List / Search:** Let an admin user browse all enrolled customers, or search for one
  by mobile number or name, with pagination.

---

## 2. Scope

| Method | Path | Flow | Description |
|---|---|---|---|
| POST | `/admin/accounts/{accountId}/freeze` | 9 | Freeze a loyalty account |
| POST | `/admin/accounts/{accountId}/unfreeze` | 9 | Reactivate a frozen account |
| POST | `/admin/accounts/{accountId}/adjustments` | 8 | Manually credit or debit an account |
| GET | `/admin/customers` | — | List/search customers with pagination |

---

## 3. Package structure

```
com.aman.acceptance.loyalty
├── controller
│   ├── AdminAccountController.java          (freeze, unfreeze, and adjustments endpoints)
│   └── AdminCustomerController.java         (customer list/search endpoint)
├── enums
│   ├── ErrorCode.java
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
│       ├── AdjustmentResponse.java
│       ├── CustomerSummaryResponse.java
│       └── PagedResponse.java
├── repository
│   ├── LoyaltyAccountRepository.java
│   ├── AuditEventRepository.java
│   ├── LoyaltyTransactionRepository.java
│   ├── PointsLotRepository.java
│   └── CustomerRepository.java
├── service
│   ├── AccountFreezeService.java
│   ├── AdjustmentService.java
│   └── CustomerSearchService.java
└── util
    └── MobileHashUtil.java
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

### 4.4 Customer list / search — `GET /admin/customers`

Query parameters:

| Param | Required | Description |
|---|---|---|
| `search` | No | Mobile number or customer name. Omitted → returns all customers. |
| `page` | No | Zero-based page index. Default `0`. |
| `size` | No | Page size. Default `20`, max `100`. |

Example — no filter:
```
GET /admin/customers?page=0&size=10
```

Example — search by mobile:
```
GET /admin/customers?search=+201012345678
```

Example — search by name:
```
GET /admin/customers?search=ahmed
```

Success — `200 OK`:
```json
{
  "success": true,
  "data": {
    "items": [
      { "customerId": 1, "name": "Ahmed Mohamed", "status": "ACTIVE", "createdAt": "2026-08-01T10:00:00" }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "meta": {
    "correlationId": "cor-ee68f641",
    "timestamp": "2026-08-22T20:30:49.297615900Z"
  }
}
```

### 4.5 Error responses

| HTTP | Error code | When | Flow |
|---|---|---|---|
| 400 | `LOYALTY_VALIDATION_ERROR` | `reasonCode`/`actorId` missing/blank (freeze, unfreeze); `type`, `points` (must be positive), `reasonCode`, or `actorId` missing/invalid (adjustment); invalid `sort` field (customer search) | 8 & 9 |
| 404 | `LOYALTY_ACCOUNT_NOT_FOUND` | `accountId` does not exist | 8 & 9 |
| 404 | `LOYALTY_ROUTE_NOT_FOUND` | Request hits a path with no matching controller | — |
| 409 | `LOYALTY_ACCOUNT_ALREADY_FROZEN` | Freeze called on an account already `FROZEN` | 9 |
| 409 | `LOYALTY_ACCOUNT_NOT_FROZEN` | Unfreeze called on an account that is not `FROZEN` | 9 |
| 422 | `LOYALTY_INSUFFICIENT_AVAILABLE_POINTS` | Debit requested exceeds `availablePoints` | 8 |
| 423 | `LOYALTY_ACCOUNT_FROZEN` | Account is frozen — triggered when Earn/Redeem/Adjustment call `assertAccountActive()` | 8 & 9 |
| 500 | `LOYALTY_INTERNAL_ERROR` | Any genuinely unexpected error (shared fallback handler) | 8 & 9 |

---

## 5. Business rules implemented

**Freeze / Unfreeze (Flow 9)**
- An account can only be frozen from `ACTIVE` state, and unfrozen only from `FROZEN` state.
  Attempting either in the wrong state returns `409 Conflict` rather than silently succeeding.
- Every freeze/unfreeze call writes an immutable `AuditEvent` row with a before/after JSON snapshot
  of the account, plus `actorId`, `action`, `entityType`, `entityId`, and `correlationId`.
- The state change and the audit write happen inside a single `@Transactional` boundary.
- `assertAccountActive(LoyaltyAccount account)` is exposed as a public method on
  `AccountFreezeService` for other flows to call before any point mutation. It throws
  `LoyaltyException.locked(ErrorCode.LOYALTY_ACCOUNT_FROZEN, ...)` (HTTP 423) when frozen.

**Manual Adjustment (Flow 8)**
- Credit creates a new `PointsLot` with status `AVAILABLE` immediately (no 30-day lock), because a
  manual credit has no associated purchase to define a refund window.
- Debit validates `availablePoints >= requestedPoints` first, then consumes `AVAILABLE` lots FIFO by
  nearest expiry, decrementing `remainingPoints` lot by lot.
- Every adjustment writes a `LoyaltyTransaction` row (`ADJUSTMENT_CREDIT` / `ADJUSTMENT_DEBIT`), plus
  an `AuditEvent` with a before/after account snapshot.
- Before any mutation, the account is checked via `AccountFreezeService.assertAccountActive(account)` —
  reused as-is from Flow 9, not duplicated.
- The whole operation happens inside one `@Transactional` boundary.

**Customer List / Search**
- A single `search` query parameter auto-detects intent: digits/`+` only → exact match by mobile
  number (normalized, hashed with SHA-256, matched against `mobile_hash`); otherwise → partial,
  case-insensitive match by name.
- No `search` param → returns all customers, still paginated.
- Pagination defaults to page size 20, capped at 100 (`application.yml`,
  `spring.data.web.pageable.*`).

---

## 6. How to run locally

1. Ensure PostgreSQL is running and the `aman_loyalty` database exists.
2. `application.yml` has `defer-datasource-initialization: true` and `spring.sql.init.mode: always`
   so `data.sql` seeds:
    - one test account (`accountId = 1`, `ACTIVE`, `available=2500`, `locked=1000`)
    - one `EARN` transaction and one `AVAILABLE` points lot (`accountId = 1`, 2500 points)
    - four customers with real SHA-256 mobile hashes for search testing (see Section 7)
3. Run the app: `mvn spring-boot:run` (or run `Application.java` from the IDE).
4. App available at `http://localhost:8080/api/v1/loyalty`.
5. Swagger UI: `http://localhost:8080/api/v1/loyalty/swagger-ui/index.html`.

---

## 7. Testing

**Postman collection:** `Loyalty_Admin_Operations.postman_collection.json`

**Freeze / Unfreeze:**
1. Freeze — success (200)
2. Freeze — already frozen (409)
3. Unfreeze — success (200)
4. Unfreeze — not frozen (409)
5. Freeze — account not found (404)
6. Freeze — missing `reasonCode` (400)
7. Freeze — missing `actorId` (400)

**Adjustments:**
1. Credit — success (201)
2. Debit — success (201)
3. Debit — insufficient points (422)
4. Adjustment on a frozen account (423) — proves the Flow 9 guard is reused correctly
5. Account not found (404)
6. Validation — negative points (400)

**Customer search:**
1. No filter → all customers, paginated
2. Search by partial name (`ahmed`, case-insensitive) → matches multiple
3. Search by exact mobile number (both `+2010...` and `0101...` forms) → same single match
4. Search by a mobile number with no match → `200 OK`, empty `items`
5. Pagination boundaries (`page=0/1`, `size=2`)
6. Trailing-slash / unknown route → `404`, not `500`

Seed mobile numbers/hashes used for manual testing (SHA-256 of the normalized `+20...` form):

| Customer | Mobile | mobile_hash |
|---|---|---|
| Ahmed Mohamed | +201012345678 | `0895f44c7a43ae484d10b8509021516091079927afc18baa37fad1a70f35c01d` |
| Sara Ali | +201123456789 | `b1033eccac57cabbb1890d06d1459dbbe82f4eff984dfb428d163dd3dd638a59` |
| Mohamed Hassan | +201234567890 | `6386b07d492d714ac407d3f4af867a0bcdc325cb9fe63e58e585c3ce90abfd09` |
| Nour Ahmed | +201555555555 | `6afb0e2c89ae5b6f0dca979aa91327da9974d47dc320b64c79cdb724c00056d7` |

**Unit tests (Mockito):**
- `AccountFreezeServiceTest`, `AdjustmentServiceTest` 
- `CustomerSearchServiceTest` 