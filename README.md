## 1. Project Overview

The Loyalty Service provides APIs and business logic for managing a complete loyalty points lifecycle.

The main responsibilities of the system are:

- Customer management
- Loyalty program management
- Loyalty account management
- Earning loyalty points
- Redeeming loyalty points
- Reserving points during redemption
- OTP-based redemption flow
- Point locking and unlocking
- Point expiration
- Refunds and point restoration
- Point lots management
- Loyalty transaction history
- Idempotent transaction processing
- Audit events
- Admin dashboard
- Balance composition
- Points flow statistics
- OTP funnel statistics
- Recent administrative alerts

---

# 2. Technology Stack

## Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Lombok
- Spring Security
- PostgreSQL

## Database

- PostgreSQL
- JPA / Hibernate ORM
- Native SQL queries where required

## API

The application exposes REST APIs.

Example:

```text
GET /admin/balance-composition
GET /admin/points-flow
GET /admin/otp-funnel
GET /admin/alerts
````

---

# 3. Architecture

The project follows a layered architecture.

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
PostgreSQL Database
```

### Controller Layer

Responsible for:

* Receiving HTTP requests
* Validating request parameters
* Calling services
* Returning HTTP responses

Example:

```text
AdminDashboardController
```

---

### Service Layer

Responsible for:

* Business logic
* Calculations
* Transaction processing
* Point reservation
* Point redemption
* Refund handling
* Dashboard calculations

Example:

```text
AdminDashboardService
```

---

### Repository Layer

Responsible for:

* Database access
* JPA queries
* Native SQL queries
* Locking queries
* Aggregation queries

Examples:

```text
LoyaltyAccountRepository
LoyaltyTransactionRepository
RedemptionRepository
PointsLotRepository
AuditEventRepository
```

---

# 4. Main Domain Entities

The system contains several important entities.

## Customer

Represents a loyalty customer.

Important fields include:

```text
id
mobile_hash
mobile_encrypted
name
status
created_at
```

The mobile number is represented using a hash/encrypted value rather than storing the plain mobile number.

---

## Loyalty Program

Represents a loyalty program belonging to a merchant.

Important fields include:

```text
id
merchant_id
name
currency
lock_days
expiry_days
status
created_at
```

---

## Loyalty Account

Represents a customer's points account inside a loyalty program.

Important fields:

```text
id
customer_id
program_id
available_points
locked_points
reserved_points
status
version
created_at
updated_at
```

The points are separated into:

```text
available_points
locked_points
reserved_points
```

### Available Points

Points that can currently be redeemed.

### Locked Points

Points that have been earned but are still inside their lock period.

### Reserved Points

Points temporarily reserved for an ongoing redemption process.

---

# 5. Loyalty Transactions

The `loyalty_transactions` table records point movements.

Examples of transaction types:

```text
EARN
REDEEM
REFUND
```

Important fields include:

```text
id
account_id
type
source_transaction_id
points
money_amount
status
idempotency_key
original_source_transaction_id
refund_type
currency_code
transaction_time
created_at
```

Transactions provide an audit trail for point changes.

---

# 6. Idempotency

The system supports idempotent transaction processing.

An idempotency key is used to prevent duplicate processing of the same request.

The system should not depend only on:

```text
exists()
```

followed by:

```text
save()
```

because two concurrent requests may both pass the existence check.

Database constraints and transaction handling should be used to guarantee uniqueness.

---

# 7. Point Lots

Point lots represent individual batches of earned points.

Important fields include:

```text
id
account_id
earning_transaction_id
original_points
remaining_points
unlock_at
expires_at
status
version
created_at
```

A point lot allows the system to track:

* Original earned points
* Remaining points
* Unlock date
* Expiration date
* Lot status

---

# 8. Point Expiration

The system supports point expiration.

A point lot can have:

```text
unlock_at
expires_at
```

Available lots can be selected according to their expiration date.

The redemption logic uses the earliest expiring available points first.

This follows an expiration-aware allocation strategy.

---

# 9. Redemption Flow

The redemption process can be summarized as:

```text
Customer requests redemption
        |
        v
Validate account
        |
        v
Check available points
        |
        v
Reserve points
        |
        v
OTP verification
        |
        v
Authorize redemption
        |
        v
Commit redemption
        |
        v
Create loyalty transaction
        |
        v
Update point lots
        |
        v
Update loyalty account
```

---

# 10. Redemption Allocation

The system uses `PointsLot` records to determine which points are consumed.

Available lots are selected using:

```text
expires_at ASC
id ASC
```

This means points that expire earlier are consumed first.

This helps prevent unnecessary point expiration.

---

# 11. OTP Funnel

The admin dashboard provides OTP redemption funnel statistics.

The funnel contains:

```text
Reserved
    |
    v
Verified / Authorized
    |
    v
Committed
```

The system calculates these statistics from the `redemptions` table.

The dashboard currently considers the last 30 days.

---

# 12. Refunds

The system supports refund transactions.

Refunds can restore points that were previously redeemed.

The `redemption_allocations` table contains:

```text
restored_points
```

This field tracks how many points were restored during a refund.

---

# 13. Audit Events

The system contains an `audit_events` table.

Audit events are used to record important actions performed in the system.

Important fields include:

```text
id
action
actor_id
entity_type
entity_id
before_json
after_json
correlation_id
created_at
```

Example actions:

```text
EARN_POINTS
REDEEM_POINTS
REFUND_POINTS
AUTHORIZE_REDEMPTION
COMMIT_REDEMPTION
```

The audit information can be used by the admin dashboard.

---

# 14. Admin Dashboard

The Admin Dashboard provides aggregated information about the loyalty system.

## Balance Composition

Endpoint:

```http
GET /admin/balance-composition
```

Returns:

```text
available points
locked points
reserved points
expiring soon points
total owned points
```

Example response:

```json
{
  "success": true,
  "data": {
    "available": 7000,
    "locked": 3000,
    "reserved": 0,
    "expiringSoon": 0,
    "totalOwned": 10000
  }
}
```

---

## Points Flow

Endpoint:

```http
GET /admin/points-flow
```

Returns monthly loyalty point statistics.

Example:

```json
{
  "success": true,
  "data": [
    {
      "month": "2026-07",
      "issued": 2500,
      "redeemed": 1000
    }
  ]
}
```

The endpoint currently calculates data for the previous six months.

---

## OTP Funnel

Endpoint:

```http
GET /admin/otp-funnel
```

Returns:

```text
reserved
verified
committed
```

Example:

```json
{
  "success": true,
  "data": {
    "reserved": 10,
    "verified": 8,
    "committed": 7
  }
}
```

The endpoint currently uses data from the previous 30 days.

---

## Recent Alerts

Endpoint:

```http
GET /admin/alerts
```

Optional parameter:

```http
GET /admin/alerts?limit=10
```

The default limit is:

```text
10
```

Example response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "actorId": "admin",
      "action": "EARN_POINTS",
      "entityType": "LOYALTY_TRANSACTION",
      "entityId": 1,
      "afterJson": "{\"points\":2500,\"status\":\"COMMITTED\"}",
      "createdAt": "2026-08-26T19:10:00"
    }
  ]
}
```

---

# 15. Database Structure

Main tables:

```text
customers
    |
    +---- loyalty_accounts
                |
                +---- loyalty_transactions
                |
                +---- points_lots
                |
                +---- redemptions
                            |
                            +---- redemption_allocations

loyalty_programs
    |
    +---- loyalty_accounts

audit_events
```

---

# 16. Important Relationships

## Customer → Loyalty Account

One customer can have loyalty accounts.

```text
Customer
   |
   +---- LoyaltyAccount
```

---

## Loyalty Program → Loyalty Account

A loyalty account belongs to a loyalty program.

```text
LoyaltyProgram
      |
      +---- LoyaltyAccount
```

---

## Loyalty Account → Transactions

An account can contain many transactions.

```text
LoyaltyAccount
      |
      +---- LoyaltyTransaction
```

---

## Loyalty Account → Point Lots

An account can contain multiple point lots.

```text
LoyaltyAccount
      |
      +---- PointsLot
```

---

## Redemption → Allocations

A redemption can consume points from multiple point lots.

```text
Redemption
    |
    +---- RedemptionAllocation
              |
              +---- PointsLot
```

---

# 17. Concurrency Control

The system uses pessimistic database locks for critical operations.

Examples:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

This is used when modifying sensitive records such as:

```text
LoyaltyAccount
Redemption
PointsLot
```

The purpose is to prevent concurrent requests from incorrectly modifying the same points balance.

---

# 18. Loyalty Account Locking

Example:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT account
    FROM LoyaltyAccount account
    WHERE account.id = :accountId
    """)
Optional<LoyaltyAccount> findByIdForUpdate(
        @Param("accountId") Long accountId
);
```

This locks the account row during a transaction.

---

# 19. Points Lot Locking

Available lots can also be locked during redemption.

The system uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

to prevent multiple redemption requests from consuming the same points simultaneously.

---

# 20. Database Aggregation

The admin dashboard uses aggregation queries.

For example, the balance dashboard calculates:

```sql
SUM(available_points)
SUM(locked_points)
SUM(reserved_points)
```

The result is used to calculate:

```text
Total Owned Points
```

using:

```text
available + locked + reserved
```

---

# 21. Points Flow Query

Monthly point flow is calculated using:

```sql
DATE_TRUNC('month', created_at)
```

and separates transactions into:

```text
EARN
REDEEM
```

Issued points:

```sql
SUM(
    CASE
        WHEN type = 'EARN'
        THEN points
        ELSE 0
    END
)
```

Redeemed points:

```sql
SUM(
    CASE
        WHEN type = 'REDEEM'
        THEN ABS(points)
        ELSE 0
    END
)
```

---

# 22. Error Handling

The application uses a global exception handler.

Errors are returned using a consistent structure.

Example:

```json
{
  "success": false,
  "error": {
    "code": "LOYALTY_INTERNAL_ERROR",
    "message": "An unexpected error occurred. Please try again later.",
    "retryable": true
  },
  "meta": {
    "correlationId": "cor-0434ef73",
    "timestamp": "2026-08-26T16:11:07.504682800Z"
  }
}
```

The `correlationId` can be used to trace the request in application logs.

---

# 23. API Response Structure

Successful responses generally follow:

```json
{
  "success": true,
  "data": {}
}
```

The generic response class is:

```java
public class ApiResponseDto<T> {

    private boolean success;

    private T data;
}
```

---

# 24. Project Structure

Recommended project structure:

```text
src
└── main
    └── java
        └── com.aman.acceptance.loyalty
            │
            ├── controller
            │
            ├── service
            │
            ├── repository
            │
            ├── model
            │
            ├── dto
            │   ├── request
            │   └── response
            │
            ├── enums
            │
            ├── exception
            │
            ├── config
            │
            └── ...
```

---

# 25. Running the Project

## Requirements

Make sure the following are installed:

```text
Java
Maven
PostgreSQL
```

---

## Database

Create a PostgreSQL database.

Example:

```sql
CREATE DATABASE loyalty_db;
```

Configure the database connection in:

```text
application.properties
```

or:

```text
application.yml
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/loyalty_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

---

# 26. Build

Using Maven:

```bash
mvn clean install
```

---

# 27. Run

Run the application using:

```bash
mvn spring-boot:run
```

Or run the main Spring Boot application class from IntelliJ IDEA.

---

# 28. API Base URL

The application runs by default on:

```text
http://localhost:8080
```

If the application uses a context path such as:

```text
/api/v1/loyalty
```

the complete URL becomes:

```text
http://localhost:8080/api/v1/loyalty
```

---

# 29. Testing the Admin Dashboard

After starting the backend, test:

```http
GET /admin/balance-composition
```

```http
GET /admin/points-flow
```

```http
GET /admin/otp-funnel
```

```http
GET /admin/alerts?limit=10
```

---

# 30. Database Verification

Useful queries for checking the system:

## Customers

```sql
SELECT *
FROM customers;
```

## Loyalty Programs

```sql
SELECT *
FROM loyalty_programs;
```

## Loyalty Accounts

```sql
SELECT
    id,
    customer_id,
    program_id,
    available_points,
    locked_points,
    reserved_points
FROM loyalty_accounts;
```

## Loyalty Transactions

```sql
SELECT
    id,
    account_id,
    type,
    source_transaction_id,
    points,
    status,
    transaction_time,
    created_at
FROM loyalty_transactions
ORDER BY created_at DESC;
```

## Point Lots

```sql
SELECT *
FROM points_lots
ORDER BY id;
```

## Redemptions

```sql
SELECT *
FROM redemptions
ORDER BY created_at DESC;
```

## Audit Events

```sql
SELECT *
FROM audit_events
ORDER BY created_at DESC
LIMIT 10;
```

---

# 31. Checking Total Points

To verify the dashboard balance:

```sql
SELECT
    COALESCE(SUM(available_points), 0) AS available,
    COALESCE(SUM(locked_points), 0) AS locked,
    COALESCE(SUM(reserved_points), 0) AS reserved
FROM loyalty_accounts;
```

The expected calculation is:

```text
totalOwned =
    available
    + locked
    + reserved
```

---

# 32. Test Data

Example test customer:

```sql
INSERT INTO customers
(
    id,
    mobile_hash,
    mobile_encrypted,
    name,
    status,
    created_at
)
VALUES
(
    1,
    'test-hash-01',
    'encrypted-mobile-01',
    'Ahmed Test Customer',
    'ACTIVE',
    now()
)
ON CONFLICT (id) DO NOTHING;
```

Example loyalty program:

```sql
INSERT INTO loyalty_programs
(
    id,
    merchant_id,
    name,
    currency,
    lock_days,
    expiry_days,
    status,
    created_at
)
VALUES
(
    1,
    'merchant-001',
    'Test Loyalty Program',
    'EGP',
    30,
    360,
    'ACTIVE',
    now()
)
ON CONFLICT (id) DO NOTHING;
```

Example account:

```sql
INSERT INTO loyalty_accounts
(
    id,
    program_id,
    customer_id,
    available_points,
    locked_points,
    reserved_points,
    status,
    version,
    created_at,
    updated_at
)
VALUES
(
    1,
    1,
    1,
    2500,
    1000,
    0,
    'ACTIVE',
    0,
    now(),
    now()
)
ON CONFLICT (id) DO NOTHING;
```

---

# 33. Example EARN Transaction

```sql
INSERT INTO loyalty_transactions
(
    id,
    account_id,
    type,
    source_transaction_id,
    points,
    status,
    transaction_time,
    created_at
)
VALUES
(
    1,
    1,
    'EARN',
    'sale-seed-001',
    2500,
    'COMMITTED',
    now(),
    now()
)
ON CONFLICT (id) DO NOTHING;
```

---

# 34. Example Point Lot

```sql
INSERT INTO points_lots
(
    id,
    account_id,
    earning_transaction_id,
    original_points,
    remaining_points,
    unlock_at,
    expires_at,
    status,
    version,
    created_at
)
VALUES
(
    1,
    1,
    1,
    2500,
    2500,
    now() - interval '1 day',
    now() + interval '360 days',
    'AVAILABLE',
    0,
    now()
)
ON CONFLICT (id) DO NOTHING;
```

---

# 35. Important Database Constraints

The system uses database constraints to prevent duplicate transactions and maintain data integrity.

For example:

```sql
ALTER TABLE loyalty_transactions
ADD CONSTRAINT uq_refund_source_type
UNIQUE (source_transaction_id, type);
```

Database-level uniqueness is important because application-level checks alone cannot fully protect against concurrent requests.

---

# 36. Troubleshooting

## 500 Internal Server Error

If the API returns:

```json
{
  "success": false,
  "error": {
    "code": "LOYALTY_INTERNAL_ERROR"
  }
}
```

check the Spring Boot console.

Look for:

```text
Caused by:
```

or:

```text
ClassCastException
NullPointerException
ConstraintViolationException
DataIntegrityViolationException
```

---

## LocalDateTime / Timestamp Error

If the application returns:

```text
java.time.LocalDateTime cannot be cast to java.sql.Timestamp
```

make sure native query results are handled as:

```java
(LocalDateTime) row[6]
```

instead of:

```java
((java.sql.Timestamp) row[6]).toLocalDateTime()
```

---

# 37. Duplicate Mobile Hash

If PostgreSQL returns:

```text
ERROR: duplicate key value violates unique constraint
```

and:

```text
Key (mobile_hash) already exists
```

this means another customer already uses the same `mobile_hash`.

Check it using:

```sql
SELECT *
FROM customers
WHERE mobile_hash = 'YOUR_HASH';
```

Do not insert another customer using the same hash unless the business rules explicitly allow it.

---

# 38. Empty Dashboard Data

An empty dashboard does not necessarily mean that the API is broken.

For example:

```json
{
  "success": true,
  "data": []
}
```

can simply mean there are no records matching the requested period.

For example, the Points Flow endpoint only looks at the previous six months.

The OTP Funnel endpoint only looks at the previous 30 days.

---

# 39. Data Consistency

When manually inserting test data, make sure the relationships are valid.

For example:

```text
Customer
   |
   v
Loyalty Account
   |
   +---- Loyalty Transaction
   |
   +---- Points Lot
```

A transaction should reference an existing account.

A point lot should reference an existing account and earning transaction.

---

# 40. Recommended Test Scenario

A complete manual test can follow this sequence:

```text
1. Create Customer
        |
2. Create Loyalty Program
        |
3. Create Loyalty Account
        |
4. Create EARN Transaction
        |
5. Create Points Lot
        |
6. Verify Account Balance
        |
7. Start Redemption
        |
8. Reserve Points
        |
9. Verify OTP
        |
10. Commit Redemption
        |
11. Verify Transaction
        |
12. Verify Point Lot
        |
13. Verify Account Balance
        |
14. Test Refund
        |
15. Verify Restored Points
        |
16. Check Audit Events
        |
17. Check Admin Dashboard
```

---

# 41. Dashboard Verification

After inserting valid test data, verify:

```text
Balance Composition
        ↓
Available Points
Locked Points
Reserved Points
Expiring Soon
Total Owned
```

Then verify:

```text
Points Flow
        ↓
Issued
Redeemed
```

Then:

```text
OTP Funnel
        ↓
Reserved
Verified
Committed
```

Finally:

```text
Recent Alerts
        ↓
Audit Events
```

---

# 42. Important Notes

* Do not manually modify loyalty account balances unless you are preparing controlled test data.
* Prefer creating transactions through the business logic.
* Keep database constraints enabled.
* Use idempotency keys for operations that may be retried.
* Use pessimistic locking for concurrent point operations.
* Keep audit events for important business operations.
* Do not store plain customer mobile numbers when the security design requires hashing/encryption.
* Always verify account balance and point lots after redemption/refund operations.

---

# 43. Current Admin Endpoints

| Endpoint                         | Description                               |
| -------------------------------- | ----------------------------------------- |
| `GET /admin/balance-composition` | Returns loyalty point balance composition |
| `GET /admin/points-flow`         | Returns monthly issued/redeemed points    |
| `GET /admin/otp-funnel`          | Returns OTP redemption funnel             |
| `GET /admin/alerts`              | Returns recent audit events               |

---

# 44. Summary

The Loyalty Service is designed to provide a complete and consistent loyalty points lifecycle.

The core flow is:

```text
Customer
   ↓
Loyalty Program
   ↓
Loyalty Account
   ↓
Earn Points
   ↓
Points Lots
   ↓
Reserve Points
   ↓
OTP Verification
   ↓
Redeem Points
   ↓
Transaction
   ↓
Audit Event
   ↓
Admin Dashboard
```

The system also supports:

```text
Refunds
Point Restoration
Point Expiration
Point Locking
Idempotency
Concurrency Control
Audit Logging
Dashboard Aggregation
```

This architecture allows the loyalty system to safely manage point balances while providing traceability and administrative visibility.

```

**ده README على مستوى المشروع كله**، ومش مربوط بالـ `AdminDashboardService` بس.
```














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