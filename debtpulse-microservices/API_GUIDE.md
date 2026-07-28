# DebtPulse — API Flow, Validation & Test Guide

End-to-end reference for exercising the DebtPulse microservices through the **API Gateway**
(`http://localhost:9090`). Everything here goes through the gateway — the single entry point that
validates the JWT and forwards trusted identity headers to each service.

- **Aggregated Swagger UI:** `http://localhost:9090/swagger-ui.html` (pick a service from the dropdown; the
  spec uses a relative server URL so "Try it out" calls the gateway, same-origin).
- **All request bodies are JSON** (`Content-Type: application/json`).
- **All protected calls need** `Authorization: Bearer <jwt>`.

---

## 1. Authentication flow (do this first)

```
1. POST /api/auth/login            -> returns { token, userId, role, name, branchId }
2. Copy token -> send as Authorization: Bearer <token> on every subsequent call
   (in Swagger UI: click "Authorize", paste the token)
3. Token expires after 30 minutes -> log in again
```

### 1.1 Login
`POST /api/auth/login` — **public**

```json
{ "email": "admin@dp.com", "password": "password" }
```
**200 OK**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiUzI1NiJ9...",   // access token (JWT), valid 3h
  "refreshToken": "b64url-256bit...",  // rotating, single-use
  "expiresIn": 10800,                  // access-token seconds
  "userId": "USR-2026-000001",
  "role": "ADMIN",
  "name": "System Admin",
  "branchId": "B01"
}
```
**401 Unauthorized** — wrong credentials · **400 Bad Request** — invalid/missing email or password.

**Token lifecycle.** Access token = 3h JWT (validated at the gateway). Refresh token rotates on every
use and is stored hashed server-side with a 30-min sliding idle window:
- `POST /api/auth/refresh` `{ "refreshToken": "..." }` → new `{ token, refreshToken, expiresIn }` (old refresh revoked).
- `POST /api/auth/logout` `{ "refreshToken": "..." }` → revokes the session (idempotent).
Both frontends refresh silently on a 401 and retry once. A refresh token that is idle >30 min, expired,
revoked, or **replayed after rotation** (reuse → all sessions revoked) is rejected with 401.

### 1.2 Seed users (created automatically by auth-service on first start — all password `password`)

| Email | Role | Use for |
|-------|------|---------|
| `admin@dp.com` | ADMIN | everything (user mgmt, config) |
| `agent@dp.com` | COLLECTIONS_AGENT | contacts, PTPs |
| `field@dp.com` | FIELD_OFFICER | visits, collateral, asset verification |
| `legal@dp.com` | LEGAL_OFFICER | legal cases |
| `so@dp.com` | SETTLEMENT_OFFICER | settlements, restructuring |
| `l1@dp.com` / `l2@dp.com` / `l3@dp.com` | L1/L2/L3_APPROVER | approve settlements |
| `pm@dp.com` | PORTFOLIO_MANAGER | dashboards, analytics, read-all |

---

## 2. Global conventions

### 2.1 Standard error envelope (every service, every error)
```json
{
  "timestamp": "2026-07-17T10:00:00.123+05:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/auth/register",
  "fieldErrors": {
    "phone": "Phone number must contain exactly 10 digits",
    "password": "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit and a special character"
  }
}
```
`fieldErrors` appears only for validation failures; `ruleCode` appears only for business-rule violations.
`timestamp` is ISO-8601 with offset. A frontend can render `message` as a toast and map `fieldErrors`
onto form fields.

### 2.2 HTTP status codes used
| Code | When |
|------|------|
| 200 | successful GET / PATCH / PUT |
| 201 | successful POST (resource created) |
| 400 | validation failure, malformed JSON, business-rule violation, bad enum value |
| 401 | missing / invalid / expired JWT |
| 403 | authenticated but role not permitted for the operation |
| 404 | resource not found |
| 405 | wrong HTTP method on an existing path |
| 409 | data-integrity conflict (e.g. duplicate / FK constraint) |
| 415 | wrong / missing `Content-Type` |
| 500 | unexpected server error (logged with stack trace, generic message returned) |

### 2.3 Pagination (all list endpoints)
Query params: `page` (0-based, default 0), `size` (default 20), plus endpoint-specific filters.
Response shape (`PageResponse`):
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 20, "totalPages": 1, "last": true }
```

### 2.4 Validation rules reference
| Rule | Applies to | Message on failure |
|------|-----------|--------------------|
| `@CorporateEmail` | all auth emails | `Invalid email format` **or** `Email domain must be one of: dp.com` |
| `@StrongPassword` | register / reset / change password | `Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit and a special character` |
| `@Phone` | user phone, borrower phone | `Phone number must contain exactly 10 digits` |
| `@NotBlank` / `@NotNull` | required fields | `<field> is required` |
| `@Positive` / `@PositiveOrZero` | money / DPD / tenure | `<field> must be positive` / `cannot be negative` |
| `@FutureOrPresent` | scheduled visit date | `Scheduled date cannot be in the past` |
| bad enum value (e.g. `role`) | any enum field | `Invalid value 'X' for field 'role'. Allowed values: [...]` |

**Password policy:** min 8 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special (non-alphanumeric).
Valid: `Secret@123`, `Recovery#2026`. Invalid: `secret123` (no upper/special), `Ab@1` (too short).
**Phone:** exactly 10 digits, numeric only. Valid: `9876543210`. Invalid: `987654321` (9), `98765abcde`.
**Email:** must be a valid address on `dp.com`. Valid: `arjun@dp.com`. Invalid: `arjun@gmail.com`, `arjun@db.com`.

---

## 3. Endpoint reference by service

> Roles in **[brackets]** are allowed to call the endpoint. `ADMIN` may call everything in its service.
> Internal endpoints under `/api/internal/**` are **not** routed by the gateway — they are service-to-service
> (Feign) only and are listed here for completeness.

### 3.1 auth-service (`8081`)
| Method | Path | Roles | Body | Success |
|--------|------|-------|------|---------|
| POST | `/api/auth/login` | public | LoginRequest | 200 AuthResponse (access+refresh+expiresIn) |
| POST | `/api/auth/refresh` | public | `{ "refreshToken": "..." }` | 200 AuthResponse (rotated) |
| POST | `/api/auth/logout` | public | `{ "refreshToken": "..." }` | 200 (session revoked) |
| POST | `/api/auth/register` | [ADMIN] | RegisterRequest | 201 AuthResponse |
| POST | `/api/auth/forgot-password` | public | `{ "email": "agent@dp.com" }` | 200 `{message, token}` (dev returns token) |
| POST | `/api/auth/reset-password` | public | `{ "token": "...", "newPassword": "Secret@123" }` | 200 |
| POST | `/api/auth/change-password` | authenticated | `{ "currentPassword": "...", "newPassword": "Secret@123" }` | 200 |
| GET | `/api/users?page=&size=` | [ADMIN] | — | 200 Page<UserDto> |
| GET | `/api/users/{id}` | [ADMIN] | — | 200 UserDto |
| POST | `/api/users` | [ADMIN] | RegisterRequest | 201 UserDto |
| PUT | `/api/users/{id}` | [ADMIN] | UpdateUserRequest | 200 UserDto |
| PATCH | `/api/users/{id}/status?status=ACTIVE\|INACTIVE\|SUSPENDED` | [ADMIN] | — | 200 UserDto |
| DELETE | `/api/users/{id}` | [ADMIN] | — | 204 (soft-delete → INACTIVE) |
| GET | `/api/audit-logs?page=&size=` | [ADMIN, PORTFOLIO_MANAGER] | — | 200 Page<AuditLog> |
| GET | `/api/audit-logs/export/csv` | [ADMIN, PORTFOLIO_MANAGER] | — | 200 text/csv |

**RegisterRequest** (validated): `fullName`(≤100, required), `email`(dp.com, required),
`password`(strong, required), `role`(enum, required), `phone`(10 digits, required), `branchId`(optional).

### 3.2 account-service (`8082`) — Delinquent Portfolio
| Method | Path | Roles | Body | Success |
|--------|------|-------|------|---------|
| GET | `/api/accounts?page=&size=&bucket=&status=&agentId=` | [ADMIN, COLLECTIONS_AGENT] | — | 200 Page |
| GET | `/api/accounts/{id}` | [ADMIN, COLLECTIONS_AGENT] | — | 200 |
| POST | `/api/accounts` | [ADMIN, COLLECTIONS_AGENT] | CreateAccountRequest | 201 (bucket derived from `dpd`, auto-allocated) |
| POST | `/api/accounts/import/csv` | [ADMIN, COLLECTIONS_AGENT] | **multipart/form-data** `file` (Swagger shows a file picker) | 200 `{imported, failed}` |
| PUT | `/api/accounts/{id}` | [ADMIN, COLLECTIONS_AGENT] | UpdateAccountRequest | 200 |
| DELETE | `/api/accounts/{id}` | [ADMIN, COLLECTIONS_AGENT] | — | 204 |
| PATCH | `/api/accounts/{id}/assign-agent/{agentId}` | [ADMIN, COLLECTIONS_AGENT] | — | 200 |
| PATCH | `/api/accounts/{id}/status?status=` | [ADMIN, COLLECTIONS_AGENT] | — | 200 |
| POST | `/api/collateral-assets` | [ADMIN, FIELD_OFFICER] | CollateralAssetRequest | 201 |
| GET | `/api/collateral-assets/account/{accountId}` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | — | 200 List |
| PUT | `/api/collateral-assets/{id}` | [ADMIN, FIELD_OFFICER] | CollateralAssetRequest | 200 |
| GET/POST/PUT/DELETE | `/api/allocations` (+`POST /execute`) | [ADMIN, COLLECTIONS_AGENT, PORTFOLIO_MANAGER] | AllocationRuleRequest | 200/201 |
| POST | `/api/allocations/escalate` | [ADMIN, PORTFOLIO_MANAGER] | — | 200 `{reassigned}` — runs the escalation engine on demand (same logic as the nightly job) |

**CreateAccountRequest**: `loanRef`(req), `borrowerName`(req), `phone`(10 digits, optional),
`address`, `branchId`, `principalAmount`(>0, req), `totalOverdue`(≥0, req), `dpd`(≥0, req).
**UpdateAccountRequest** (PUT, all optional): `borrowerName, phone, address, branchId, principalAmount,
totalOverdue, dpd, daysInCurrentBucket, status`. `bucket` is **derived** from `dpd` (not directly editable);
`assignedAgentId`/`status` also have dedicated PATCH endpoints.
**Allocation rules are config-driven** — `AllocationRuleRequest`: `name, strategy(ROUND_ROBIN|LEAST_LOADED|
BRANCH_BASED), bucket?, targetRole, daysInBucketThreshold?, minDpd?, gracePeriodDays?, capacityLimit?,
branchId?, priority, autoEscalate, active`. The `autoEscalate` flag splits rules into two kinds evaluated
by different flows (a rule is never used by both):
- **`autoEscalate=false` (initial-allocation rule)** — used at import time (`autoAllocate`) and by
  `POST /api/allocations/execute` to place fresh/unassigned accounts onto the matching `targetRole`
  (normally `COLLECTIONS_AGENT`) via the rule's strategy/branch/capacity. When no allocation rule matches,
  an account falls back to the least-loaded active collections agent so imports are never left unassigned.
- **`autoEscalate=true` (escalation rule)** — used only by the escalation job (`POST /api/allocations/escalate`
  on demand, or the scheduled run) to move an account that has stagnated in a bucket up to a higher `targetRole`.
For both flows the highest-`priority` matching rule wins; capacity, grace period, branch scope and the PTP-guard
are honoured, the new owner is notified (`ESCALATION` category), and already-correctly-assigned accounts are
skipped (idempotent). The escalation cadence is externalised via `allocation.escalation.cron` (default `0 0 2 * * ?`).

### 3.3 contact-service (`8083`) — Contact & Follow-Up
| Method | Path | Roles | Body |
|--------|------|-------|------|
| POST | `/api/contacts` | [ADMIN, PORTFOLIO_MANAGER, COLLECTIONS_AGENT] | ContactAttemptRequest → 201 |
| GET | `/api/contacts?page=&size=&accountId=` | [ADMIN, COLLECTIONS_AGENT, PORTFOLIO_MANAGER] | — |
| PUT | `/api/contacts/{id}` | [ADMIN, PORTFOLIO_MANAGER] | ContactAttemptRequest |
| POST | `/api/ptp` | [ADMIN, COLLECTIONS_AGENT] | PtpRequest → 201 |
| GET | `/api/ptp?page=&size=&accountId=` | [ADMIN, COLLECTIONS_AGENT, PORTFOLIO_MANAGER] | — |
| PATCH | `/api/ptp/{id}/payment?actualPaidAmount=` | [ADMIN, COLLECTIONS_AGENT] | — (KEPT if covers amount, else PARTIAL) |
| PATCH | `/api/ptp/{id}/reschedule?commitmentDate=YYYY-MM-DD` | [ADMIN, COLLECTIONS_AGENT] | — |
| POST | `/api/borrower-contacts` | [ADMIN, COLLECTIONS_AGENT] | BorrowerContactRequest → 201 |
| GET/PUT/DELETE | `/api/borrower-contacts/**` | [ADMIN, COLLECTIONS_AGENT (+PM read)] | — |

**ContactAttemptRequest**: `accountId`(req), `agentId`(optional — inferred for agents), `contactDate`,
`channel`(enum req), `outcome`(enum req), `notes`.
**PtpRequest**: `accountId`(req), `agentId`(optional), `ptpDate`(req), `ptpAmount`(>0 req), `commitmentDate`(req).
**BorrowerContactRequest**: `accountId`(req), `contactType`(enum req), `name`(req), `phone`(10 digits, req), `relationship`, `status`.

### 3.4 field-service (`8084`) — Field Recovery
| Method | Path | Roles | Body |
|--------|------|-------|------|
| POST | `/api/visits` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | ScheduleVisitRequest → 201 |
| GET | `/api/visits?page=&size=&status=` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | — |
| GET | `/api/visits/my-visits` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | — (visits for the caller) |
| PATCH | `/api/visits/{id}/complete` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | CompleteVisitRequest |
| PATCH | `/api/visits/{id}/missed` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | — |
| POST | `/api/asset-verifications` | [ADMIN, FIELD_OFFICER] | AssetVerificationRequest → 201 (flags collateral VERIFIED) |
| GET | `/api/asset-verifications?page=&size=` | [ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER] | — |

**ScheduleVisitRequest**: `accountId`(req), `officerId`(req), `scheduledDate`(today or future, req), `nextActionRequired`.
Scheduling validates the officer **exists, is ACTIVE, and has role FIELD_OFFICER** (400 otherwise).
**Object-level authorization:** `complete`/`missed` enforce ownership — a FIELD_OFFICER may only modify a
visit **assigned to them** (else **403**); ADMIN/PORTFOLIO_MANAGER may modify any. (Fixes the IDOR where any
officer could edit any visit.)
**AssetVerificationRequest**: `visitId`(req), `assetId`(req), `condition`(enum req), `currentLocation`,
`realisableValue`(≥0), `remarks`, `verifiedById`, `verificationDate`.

### 3.5 settlement-service (`8085`) — Settlement & Restructuring
| Method | Path | Roles | Body |
|--------|------|-------|------|
| POST | `/api/settlements` | [ADMIN, SETTLEMENT_OFFICER] | SettlementRequest → 201 (haircut computed) |
| PATCH | `/api/settlements/{id}/submit` | [ADMIN, SETTLEMENT_OFFICER, PORTFOLIO_MANAGER] | — (Draft → PendingApproval) |
| POST | `/api/settlements/{id}/decide?level=L1\|L2\|L3` | ADMIN + the approver role for the **current step** | ApprovalDecisionRequest; `level` query param (enum → Swagger dropdown) must match the current step |
| PATCH | `/api/settlements/{id}/mark-paid` | [ADMIN, SETTLEMENT_OFFICER, PORTFOLIO_MANAGER] | — |
| GET | `/api/settlements` `/outstanding` `/past-deadline` `/approval-queue` `/{id}` | [ADMIN, SO, PM, L1/L2/L3] | — |
| POST | `/api/restructuring` | [ADMIN, SETTLEMENT_OFFICER] | RestructuringRequest → 201 |
| PATCH | `/api/restructuring/{id}/approve` `/reject` | [ADMIN, L1/L2/L3_APPROVER] | — |
| GET | `/api/restructuring` `/{id}` `/account/{accountId}` | [ADMIN, SETTLEMENT_OFFICER, L1/L2/L3, PORTFOLIO_MANAGER] | — |

**SettlementRequest**: `accountId`(req), `totalOutstanding`(>0 req), `settlementAmount`(>0 req),
`paymentDeadline`(req), `notes`(optional, ≤1000). **The client does NOT send an approval level** — the
haircut % is computed server-side and the required approval chain is derived from it (`<10%`→L1;
`10–25%`→L1→L2; `≥25%`→L1→L2→L3) and approved **sequentially**. The create response returns
`haircutPercent`, `requiredApprovalChain`, `currentStep` and `status`.
**Decide** takes `level` as a **query parameter** (`?level=L1|L2|L3` — an enum, so Swagger shows it as a dropdown), plus the body **ApprovalDecisionRequest**: `decision`(APPROVE/REJECT req), `comments`(≤1000). The action applies to the proposal's `currentStep`; the `level` param is validated against it and a mismatch returns **400 `APPROVAL_LEVEL_MISMATCH`** ("You selected L1 but this settlement is awaiting L2 approval").
**RestructuringRequest**: `accountId`(req), `revisedTenure`(>0 req), `revisedEmi`(>0 req), `waiverAmount`(≥0 req), `startDate`(req).

### 3.6 legal-service (`8086`) — Legal Proceedings — [ADMIN, LEGAL_OFFICER, PORTFOLIO_MANAGER]
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/legal/cases` | LegalCaseRequest → 201 ([ADMIN, LEGAL_OFFICER]) |
| GET/PUT | `/api/legal/cases` `/cases/{id}` | list/read/update |
| POST | `/api/legal/hearings` | CourtHearingRequest → 201 |
| GET | `/api/legal/cases/{caseId}/hearings` | list hearings |
| POST | `/api/legal/orders` | RecoveryOrderRequest → 201 |
| GET/DELETE | `/api/legal/orders` `/orders/{id}` | list/read/delete |

**LegalCaseRequest**: `accountId`(req), `caseType`(enum req), `filingDate`(req), `courtName`(req), `caseNumber`(req), `status`.
**CourtHearingRequest**: `caseId`(req), `hearingDate`(req), `hearingOutcome`(enum req), `nextHearingDate`, `notes`.
**RecoveryOrderRequest**: `caseId`(req), `orderType`(enum req), `issuedDate`(req), `executionDeadline`(req), `status`.

### 3.7 analytics-service (`8087`) — [ADMIN, PORTFOLIO_MANAGER] — read-only dashboards
`GET /api/analytics/dashboard | bucket-distribution | ptp-metrics | settlement-metrics | recovery-rate |
bucket-migration | cash-collected | field-visit-success | legal-conversion`,
`POST /api/analytics/reports/generate?scope=`, `GET /api/analytics/reports`.
> Aggregates over Feign from every service; if a source is down the circuit breaker returns safe zeros
> (dashboard still renders).

### 3.8 notification-service (`8088`) — the caller's own notifications
`GET /api/notifications?page=&size=`, `GET /api/notifications/{id}`, `GET /api/notifications/unread-count`,
`PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/{id}/dismiss`, `PATCH /api/notifications/read-all`.

---

## 4. Test-case matrix (run these to verify the whole system)

### 4.1 Auth & security
| # | Request | Expected |
|---|---------|----------|
| A1 | `POST /api/auth/login` `admin@dp.com`/`password` | 200 + token |
| A2 | login with wrong password | 401 |
| A3 | login `email:"not-an-email"` | 400 `fieldErrors.email = Invalid email format` |
| A4 | any protected GET with **no** `Authorization` | 401 (gateway) |
| A5 | protected GET with expired/garbage token | 401 |
| A6 | `agent@dp.com` calls `POST /api/auth/register` | 403 (ADMIN only) |
| A7 | preflight `OPTIONS /api/auth/login` from a browser origin | 200 (CORS, no auth) |

### 4.2 Registration validation (the reported bug — now enforced)
| # | Body (`POST /api/auth/register` as ADMIN) | Expected |
|---|-------------------------------------------|----------|
| R1 | full valid: `Secret@123` / `9876543210` / `x@dp.com` | 201 |
| R2 | `password:"arjun@g9"` (7 chars, no upper) | 400 `fieldErrors.password` |
| R3 | `password:"secret123"` (no upper, no special) | 400 `fieldErrors.password` |
| R4 | `phone:"987654321"` (9 digits) | 400 `fieldErrors.phone` |
| R5 | `phone:"98765abcde"` | 400 `fieldErrors.phone` |
| R6 | `email:"arjun@gmail.com"` | 400 `fieldErrors.email = Email domain must be one of: dp.com` |
| R7 | `role:"MANAGER"` (bad enum) | 400 `Invalid value 'MANAGER' ... Allowed values [...]` |
| R8 | missing `fullName` | 400 `fieldErrors.fullName = Full name is required` |

### 4.3 Business rules & lifecycle (examples)
| # | Request | Expected |
|---|---------|----------|
| B1 | `GET /api/accounts/DOES-NOT-EXIST` | 404 `Delinquent account not found` |
| B2 | `POST /api/accounts` `principalAmount:-5` | 400 `fieldErrors.principalAmount` |
| B3 | `POST /api/settlements` `settlementAmount > totalOutstanding` | 400 business rule (+`ruleCode`) |
| B4 | approve a settlement at the wrong level | 400/403 per rule |
| B5 | `POST /api/visits` `scheduledDate` in the past | 400 `Scheduled date cannot be in the past` |
| B6 | `GET /api/accounts` with `GET` on `POST`-only `/api/accounts` variant | 405 |
| B7 | POST with `Content-Type: text/plain` | 415 |

---

## 5. Sample dataset (~20 accounts + supporting records)

Seed in this **order** (each step uses IDs returned by the previous). IDs below (`ACC-01`…) are logical
labels — the API returns human-readable business IDs like `ACC-2026-000001` (see §8); substitute the
real returned id in later calls.

### 5.1 Extra users to register (`POST /api/auth/register` as ADMIN) — all `password: "Secret@123"`
```json
[
  { "fullName": "Arjun Gupta",   "email": "arjun@dp.com",  "password": "Secret@123", "role": "COLLECTIONS_AGENT", "phone": "9876543210", "branchId": "B01" },
  { "fullName": "Meera Nair",    "email": "meera@dp.com",  "password": "Secret@123", "role": "COLLECTIONS_AGENT", "phone": "9876543211", "branchId": "B02" },
  { "fullName": "Ravi Kumar",    "email": "ravi@dp.com",   "password": "Secret@123", "role": "FIELD_OFFICER",     "phone": "9876543212", "branchId": "B01" },
  { "fullName": "Sana Sheikh",   "email": "sana@dp.com",   "password": "Secret@123", "role": "LEGAL_OFFICER",     "phone": "9876543213", "branchId": "B02" },
  { "fullName": "Vikram Rao",    "email": "vikram@dp.com", "password": "Secret@123", "role": "SETTLEMENT_OFFICER", "phone": "9876543214", "branchId": "B01" }
]
```

### 5.2 Twenty delinquent accounts (`POST /api/accounts`) — buckets derived from `dpd`
```json
[
  { "loanRef": "LN-1001", "borrowerName": "Anil Sharma",   "phone": "9800000001", "address": "12 MG Road, Pune",        "branchId": "B01", "principalAmount": 250000, "totalOverdue": 18000,  "dpd": 22 },
  { "loanRef": "LN-1002", "borrowerName": "Beena Thomas",  "phone": "9800000002", "address": "45 Residency Rd, Kochi",  "branchId": "B02", "principalAmount": 120000, "totalOverdue": 9500,   "dpd": 41 },
  { "loanRef": "LN-1003", "borrowerName": "Chetan Mehta",  "phone": "9800000003", "address": "7 Linking Rd, Mumbai",    "branchId": "B01", "principalAmount": 540000, "totalOverdue": 62000,  "dpd": 75 },
  { "loanRef": "LN-1004", "borrowerName": "Divya Menon",   "phone": "9800000004", "address": "9 Brigade Rd, Bengaluru", "branchId": "B03", "principalAmount": 300000, "totalOverdue": 41000,  "dpd": 103 },
  { "loanRef": "LN-1005", "borrowerName": "Esha Kapoor",   "phone": "9800000005", "address": "22 Park St, Kolkata",     "branchId": "B02", "principalAmount": 890000, "totalOverdue": 150000, "dpd": 168 },
  { "loanRef": "LN-1006", "borrowerName": "Farhan Ali",    "phone": "9800000006", "address": "3 Banjara Hills, Hyd",    "branchId": "B01", "principalAmount": 210000, "totalOverdue": 12000,  "dpd": 15 },
  { "loanRef": "LN-1007", "borrowerName": "Gita Reddy",    "phone": "9800000007", "address": "88 Anna Salai, Chennai",  "branchId": "B03", "principalAmount": 460000, "totalOverdue": 58000,  "dpd": 55 },
  { "loanRef": "LN-1008", "borrowerName": "Harish Patel",  "phone": "9800000008", "address": "14 CG Road, Ahmedabad",   "branchId": "B02", "principalAmount": 175000, "totalOverdue": 22000,  "dpd": 88 },
  { "loanRef": "LN-1009", "borrowerName": "Irfan Khan",    "phone": "9800000009", "address": "5 Hazratganj, Lucknow",   "branchId": "B01", "principalAmount": 620000, "totalOverdue": 99000,  "dpd": 121 },
  { "loanRef": "LN-1010", "borrowerName": "Jaya Iyer",     "phone": "9800000010", "address": "31 FC Road, Pune",        "branchId": "B03", "principalAmount": 330000, "totalOverdue": 47000,  "dpd": 190 },
  { "loanRef": "LN-1011", "borrowerName": "Kiran Bose",    "phone": "9800000011", "address": "2 Salt Lake, Kolkata",    "branchId": "B02", "principalAmount": 145000, "totalOverdue": 8000,   "dpd": 28 },
  { "loanRef": "LN-1012", "borrowerName": "Lata Joshi",    "phone": "9800000012", "address": "67 JM Road, Pune",        "branchId": "B01", "principalAmount": 500000, "totalOverdue": 70000,  "dpd": 66 },
  { "loanRef": "LN-1013", "borrowerName": "Manoj Verma",   "phone": "9800000013", "address": "18 Civil Lines, Jaipur",  "branchId": "B03", "principalAmount": 275000, "totalOverdue": 35000,  "dpd": 95 },
  { "loanRef": "LN-1014", "borrowerName": "Neha Sinha",    "phone": "9800000014", "address": "9 Boring Rd, Patna",      "branchId": "B02", "principalAmount": 410000, "totalOverdue": 60000,  "dpd": 112 },
  { "loanRef": "LN-1015", "borrowerName": "Omar Farooq",   "phone": "9800000015", "address": "40 Dalal St, Mumbai",     "branchId": "B01", "principalAmount": 720000, "totalOverdue": 130000, "dpd": 175 },
  { "loanRef": "LN-1016", "borrowerName": "Pooja Rane",    "phone": "9800000016", "address": "6 MI Road, Jaipur",       "branchId": "B03", "principalAmount": 160000, "totalOverdue": 11000,  "dpd": 18 },
  { "loanRef": "LN-1017", "borrowerName": "Qadir Beg",     "phone": "9800000017", "address": "77 Mall Rd, Shimla",      "branchId": "B02", "principalAmount": 385000, "totalOverdue": 52000,  "dpd": 59 },
  { "loanRef": "LN-1018", "borrowerName": "Rekha Das",     "phone": "9800000018", "address": "23 Camac St, Kolkata",    "branchId": "B01", "principalAmount": 240000, "totalOverdue": 30000,  "dpd": 84 },
  { "loanRef": "LN-1019", "borrowerName": "Suresh Pillai", "phone": "9800000019", "address": "10 MG Rd, Kochi",         "branchId": "B03", "principalAmount": 560000, "totalOverdue": 88000,  "dpd": 118 },
  { "loanRef": "LN-1020", "borrowerName": "Tara Bhat",     "phone": "9800000020", "address": "15 Church St, Bengaluru", "branchId": "B02", "principalAmount": 305000, "totalOverdue": 44000,  "dpd": 205 }
]
```
Expected bucket mapping from `dpd`: ≤30 → X30, ≤60 → X60, ≤90 → X90, ≤120 → X120, ≤180 → X180, else NPA.
(So LN-1005/1015 → X180, LN-1010/1020 → NPA, LN-1001/1006/1011/1016 → X30, etc.)

### 5.3 Supporting records (use returned ids)
```jsonc
// Collateral (POST /api/collateral-assets)  — field@dp.com
{ "accountId": "<ACC-03>", "assetType": "PROPERTY", "description": "2BHK flat, Mumbai", "estimatedValue": 4500000 }
{ "accountId": "<ACC-15>", "assetType": "VEHICLE",  "description": "Truck MH-12-AB",     "estimatedValue": 900000 }

// Borrower contact (POST /api/borrower-contacts)  — agent@dp.com
{ "accountId": "<ACC-01>", "contactType": "PRIMARY", "name": "Anil Sharma", "phone": "9800000001", "relationship": "Self" }

// Contact attempt (POST /api/contacts)  — agent@dp.com
{ "accountId": "<ACC-01>", "channel": "CALL", "outcome": "CONNECTED", "notes": "Promised to pay by month end" }

// Promise-to-pay (POST /api/ptp)  — agent@dp.com
{ "accountId": "<ACC-01>", "ptpDate": "2026-07-17", "ptpAmount": 15000, "commitmentDate": "2026-07-31" }

// Field visit (POST /api/visits)  — field@dp.com
{ "accountId": "<ACC-03>", "officerId": "<RAVI_ID>", "scheduledDate": "2026-07-20", "nextActionRequired": "Verify collateral" }

// Settlement (POST /api/settlements)  — so@dp.com
{ "accountId": "<ACC-05>", "totalOutstanding": 150000, "settlementAmount": 105000, "paymentDeadline": "2026-08-15", "notes": "One-time settlement" }   // 30% haircut → chain L1→L2→L3

// Legal case (POST /api/legal/cases)  — legal@dp.com
{ "accountId": "<ACC-15>", "caseType": "SARFAESI_ACTION", "filingDate": "2026-07-10", "courtName": "DRT Mumbai", "caseNumber": "DRT/2026/114" }
```

### 5.4 Quick verification after seeding
- `GET /api/accounts?size=20` → 20 accounts, `bucket` correctly derived, agents auto-assigned.
- `GET /api/analytics/dashboard` (pm@dp.com) → non-zero totals, bucket distribution, PTP/settlement metrics.
- `GET /api/notifications` (the officer) → alerts raised by settlement/legal/PTP flows.
- `GET /api/audit-logs` (admin) → create/update actions recorded with user + timestamp.

---

## 6. Schedulers (automatic background jobs)
| Service | Job | Cron | Effect |
|---------|-----|------|--------|
| account | DPD ageing | `0 30 0 * * ?` (00:30 daily) | +1 DPD per ACTIVE account, re-buckets, updates days-in-bucket |
| account | Escalation | (daily) | escalates accounts breaching bucket-time thresholds per allocation rules |
| contact | PTP breach sweep | `0 0 8 * * ?` (08:00 daily) | ACTIVE PTPs past commitment date → BROKEN + notify agent (category PTP) |
| field | Visit reminders | (daily) | reminds officers of upcoming/overdue visits |
| legal | Hearing alerts | (daily) | alerts officers of upcoming hearing dates |
| settlement | Expiry sweep | `0 0 1 * * ?` (01:00 daily) | APPROVED settlements past deadline → EXPIRED + notify officer |

All jobs log start/finish, run in their own transaction, and call other services via Feign as
`SYSTEM` (audited). If a downstream service is unavailable the Resilience4j fallback logs a warning and
the job continues — a dropped notification never rolls back the primary state change.

---

## 7. Business ID scheme

Every entity id is a human-readable, chronologically-sortable **business ID** of the form
`PREFIX-YEAR-NNNNNN` (6-digit, zero-padded, sequence resets each calendar year), e.g. `ACC-2026-000001`.

| Entity | Prefix | Example | Entity | Prefix | Example |
|--------|--------|---------|--------|--------|---------|
| User | `USR` | `USR-2026-000001` | Settlement proposal | `SET` | `SET-2026-000001` |
| Audit log | `AUD` | `AUD-2026-000001` | Restructuring proposal | `RST` | `RST-2026-000001` |
| Delinquent account | `ACC` | `ACC-2026-000001` | Approval step | `APS` | `APS-2026-000001` |
| Collateral asset | `COL` | `COL-2026-000001` | Legal case | `LEG` | `LEG-2026-000001` |
| Allocation rule | `ALR` | `ALR-2026-000001` | Court hearing | `HRG` | `HRG-2026-000001` |
| Contact attempt | `CON` | `CON-2026-000001` | Recovery order | `ORD` | `ORD-2026-000001` |
| Promise-to-pay | `PTP` | `PTP-2026-000001` | Notification | `NOT` | `NOT-2026-000001` |
| Borrower contact | `BOR` | `BOR-2026-000001` | Recovery report | `RPT` | `RPT-2026-000001` |
| Field visit | `VIS` | `VIS-2026-000001` | Asset verification | `AVR` | `AVR-2026-000001` |

- Implemented once, centrally, by the shared `@BusinessId(prefix = "…")` Hibernate generator
  (`com.debtpulse.common.id`). Applying it to an entity's `@Id` is all a service needs to do.
- Allocation is a single atomic MySQL upsert against each service's `id_sequence` table, so ids stay
  unique even across many concurrent instances. Sequences may have gaps (rolled-back inserts) but never
  duplicates. The generator also honours an explicitly assigned id (used by imports/tests); the seed
  users flow through the generator normally and receive `USR-2026-0000NN` ids in seed order (admin first).

## 8. Resilience & fallbacks (what a frontend sees when a service is down)
- Inter-service reads (Feign + Resilience4j) fall back to safe defaults: `null` / empty list / `false` /
  zero, with a logged warning — so, e.g., the analytics dashboard still renders with zeros rather than 500ing.
- Notifications are best-effort: if notification-service is down the action still succeeds; the alert is dropped (logged).
- The gateway returns a JSON `{ "error": "..." }` (401) for auth failures before the request reaches a service.

## 9. Observability
- **Correlation id** — every request gets an `X-Correlation-Id` (sent by the client or generated at the
  edge), echoed on the response, propagated across Feign calls, and printed in every log line
  (`[correlationId]`). Pass your own header to trace a flow end-to-end.
- **Audit** — business actions annotated `@Auditable` emit a structured `AUDIT action=… user=… entity=…
  outcome=… correlationId=…` line (login/logout/refresh are wired; the framework is reusable across services).
- **Actuator** per service: `GET /actuator/health` (with `/health/liveness` and `/health/readiness` probes —
  readiness reflects DB + circuit-breaker health), `/actuator/metrics`, `/actuator/info`, `/actuator/circuitbreakers`.
  `/actuator/prometheus` activates once `micrometer-registry-prometheus` is added (online-only jar).
