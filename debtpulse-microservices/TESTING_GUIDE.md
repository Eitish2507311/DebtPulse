# DebtPulse — Detailed API Testing Guide

Exhaustive, per-endpoint test guide with request bodies, expected responses, and a sequenced flow so
each module can be verified in order and then cross-checked. Everything goes through the **API Gateway**
`http://localhost:9090`. Pair this with `API_GUIDE.md` (validation rules + 20-record sample dataset).

**Legend:** ✅ happy path · ⛔ negative case · 🔗 depends on a prior step's id.
Every protected call needs `Authorization: Bearer <token>` and (for bodies) `Content-Type: application/json`.

---

## 0. Setup & smoke (do before anything else)

| Step | Action | Expected |
|------|--------|----------|
| 0.1 | Start config-server(8888) → eureka(8761) → api-gateway(9090) → all 8 services | all `UP` |
| 0.2 | `GET http://localhost:8761` | Eureka lists 9 apps registered |
| 0.3 | `GET http://localhost:9090/actuator/health` | `{"status":"UP"}` |
| 0.4 | Open `http://localhost:9090/swagger-ui.html` | dropdown lists all 8 services; specs load (no 401) |
| 0.5 | Verify each schema created a sequence table, e.g. `SHOW TABLES IN debtpulse_account LIKE 'id_sequence'` | 1 row |

**Token matrix** — log in as each role once (§1.1) and keep the tokens handy:
`ADMIN, COLLECTIONS_AGENT, FIELD_OFFICER, LEGAL_OFFICER, SETTLEMENT_OFFICER, L1/L2/L3_APPROVER, PORTFOLIO_MANAGER`.

---

## 1. MODULE: Auth & Identity (auth-service)

### 1.1 `POST /api/auth/login` — public
✅ `{ "email": "admin@dp.com", "password": "password" }` → **200**
```json
{ "message": "Login successful", "token": "<jwt 3h>", "refreshToken": "<rotating>", "expiresIn": 10800,
  "userId": "USR-2026-000001", "role": "ADMIN", "name": "System Admin", "branchId": "B01" }
```
⛔ wrong password → **401** `{ "error": "Invalid email or password" }`
⛔ `email:"not-an-email"` → **400** `fieldErrors.email = "Invalid email format"`
⛔ `email:"ghost@dp.com"` (unknown) → **401**
⛔ missing `password` → **400** `fieldErrors.password = "Password is required"`
⛔ `email:"x@gmail.com"` → **400** `fieldErrors.email = "Email domain must be one of: dp.com"`

> Repeat 1.1 for `agent@dp.com`, `field@dp.com`, `legal@dp.com`, `so@dp.com`, `l1@dp.com`, `l2@dp.com`,
> `l3@dp.com`, `pm@dp.com` (all password `password`) → 200 with the matching role.

### 1.1b Refresh & logout — public (refresh-token session)
- ✅ `POST /api/auth/refresh` `{ "refreshToken": "<from login>" }` → 200 with a **new** `token`+`refreshToken`.
  ⛔ reuse the **old** refresh token again → **401** (single-use; rotation) — and all that user's sessions are revoked (reuse = compromise).
  ⛔ garbage / expired / >30-min-idle refresh token → **401**.
- ✅ `POST /api/auth/logout` `{ "refreshToken": "<current>" }` → 200; the refresh token no longer works (⛔ 401 on next refresh). Idempotent (logging out an unknown token still returns 200).
- ✅ **silent refresh** (UI): let an access token expire, make any call → the SPA refreshes once and retries transparently; when refresh fails it redirects to login.

### 1.2 `POST /api/auth/register` — **[ADMIN]**, 201
✅ (ADMIN token)
```json
{ "fullName": "Arjun Gupta", "email": "arjun@dp.com", "password": "Secret@123", "role": "COLLECTIONS_AGENT", "phone": "9876543210", "branchId": "B01" }
```
→ **201** `{ ... "userId": "USR-2026-0000NN" }` (increments each call)
⛔ no token → **401** · ⛔ AGENT token → **403**
⛔ `password:"secret123"` → **400** `fieldErrors.password`
⛔ `password:"arjun@g9"` (7 chars) → **400** `fieldErrors.password`
⛔ `phone:"987654321"` (9) / `"98765abcde"` → **400** `fieldErrors.phone`
⛔ `email:"arjun@gmail.com"` → **400** `fieldErrors.email`
⛔ `role:"MANAGER"` (bad enum) → **400** `"Invalid value 'MANAGER' ... Allowed values [...]"`
⛔ missing `fullName` → **400** `fieldErrors.fullName`
⛔ duplicate email (register `arjun@dp.com` again) → **409** or business error (unique email)

### 1.3 Password flows — public/authenticated
- ✅ `POST /api/auth/forgot-password` `{ "email": "agent@dp.com" }` → **200** `{ "message": "...", "token": "<reset-token>" }` (dev returns token). 🔗 keep token.
  ⛔ `email:"x@gmail.com"` → **400**.
- ✅ `POST /api/auth/reset-password` `{ "token": "<reset-token>", "newPassword": "Reset@2026" }` → **200**. 🔗 then login with new password → 200.
  ⛔ `token:"bad"` → **400/404** · ⛔ `newPassword:"weak"` → **400** `fieldErrors.newPassword`.
- ✅ `POST /api/auth/change-password` (any auth token) `{ "currentPassword": "password", "newPassword": "Change@2026" }` → **200**.
  ⛔ wrong `currentPassword` → **400/403** · ⛔ weak `newPassword` → **400**.

### 1.4 User management — **[ADMIN]** (`/api/users`)
| Case | Request | Expected |
|------|---------|----------|
| ✅ list | `GET /api/users?page=0&size=20` | 200 `PageResponse<UserDto>` (never exposes `passwordHash`) |
| ✅ get 🔗 | `GET /api/users/{userId}` | 200 UserDto |
| ⛔ get missing | `GET /api/users/USR-9999` | 404 |
| ✅ create | `POST /api/users` (same body as 1.2) | 201 UserDto |
| ✅ update 🔗 | `PUT /api/users/{id}` `{ "fullName":"Arjun G", "phone":"9811111111" }` | 200 (null fields unchanged) |
| ⛔ update bad phone | `PUT` `{ "phone":"123" }` | 400 `fieldErrors.phone` |
| ✅ status 🔗 | `PATCH /api/users/{id}/status?status=SUSPENDED` | 200 (status=SUSPENDED) |
| ⛔ bad status | `?status=FROZEN` | 400 bad enum |
| ✅ delete 🔗 | `DELETE /api/users/{id}` | 204 (soft-delete → INACTIVE) |
| ⛔ RBAC | any of the above with AGENT token | 403 |

### 1.5 Audit logs — **[ADMIN, PORTFOLIO_MANAGER]** (`/api/audit-logs`)
- ✅ `GET /api/audit-logs?page=0&size=20` → 200 (entries accumulate as you create/update elsewhere).
- ✅ `GET /api/audit-logs/{auditId}` → 200 · ⛔ missing → 404.
- ✅ `GET /api/audit-logs/user/{userId}?page=0&size=20` → 200.
- ✅ `GET /api/audit-logs/entity/DelinquentAccount/{accountId}` → 200 List (run after §2).
- ✅ `GET /api/audit-logs/export/csv` → 200 `text/csv`.
- ⛔ AGENT token on any → 403.

### 1.6 Util — `GET /api/util/hash?raw=password` — public → 200 `{ "hash": "$2a$..." }`.

---

## 2. MODULE: Delinquent Portfolio (account-service)

### 2.1 `POST /api/accounts` — **[ADMIN, COLLECTIONS_AGENT]**, 201
✅
```json
{ "loanRef":"LN-1001", "borrowerName":"Anil Sharma", "phone":"9800000001", "address":"12 MG Road, Pune", "branchId":"B01", "principalAmount":250000, "totalOverdue":18000, "dpd":22 }
```
→ **201** `{ "accountId":"ACC-2026-000001", "bucket":"X30", "status":"ACTIVE", "assignedAgentId": "<auto>" ... }`
🔗 Post all 20 records from `API_GUIDE.md §5.2`. Confirm bucket mapping: dpd 22→X30, 41→X60, 75→X90, 103→X120, 168→X180, 205→NPA.
⛔ `principalAmount:-5` → 400 `fieldErrors.principalAmount` · ⛔ `dpd:-1` → 400 · ⛔ missing `loanRef` → 400
⛔ `phone:"12345"` → 400 `fieldErrors.phone` · ⛔ FIELD_OFFICER token → 403 · ⛔ no token → 401

### 2.2 Read / filter — **[ADMIN, COLLECTIONS_AGENT]**
- ✅ `GET /api/accounts?page=0&size=20` → 200 Page (sorted by DPD desc).
- ✅ filters: `?bucket=NPA`, `?status=ACTIVE`, `?agentId=<id>` → filtered results.
- ✅ `GET /api/accounts/{accountId}` 🔗 → 200 · ⛔ `GET /api/accounts/ACC-9999` → 404.

### 2.3 Mutations 🔗
| Case | Request | Expected |
|------|---------|----------|
| ✅ update | `PUT /api/accounts/{id}` `{ "totalOverdue":20000, "dpd":35 }` | 200 (bucket re-derived → X60) |
| ✅ update days-in-bucket | `PUT /api/accounts/{id}` `{ "daysInCurrentBucket":12 }` | 200; value persists (previously silently dropped — now editable) |
| ⛔ negative days-in-bucket | `{ "daysInCurrentBucket":-1 }` | 400 `fieldErrors.daysInCurrentBucket` |
| ✅ assign agent | `PATCH /api/accounts/{id}/assign-agent/{agentId}` | 200 (assignedAgentId set) |
| ✅ status | `PATCH /api/accounts/{id}/status?status=LEGAL` | 200 |
| ⛔ bad status | `?status=FOO` | 400 |
| ✅ delete | `DELETE /api/accounts/{id}` | 204 |
| ✅ CSV import | `POST /api/accounts/import/csv` — **multipart/form-data**; Swagger shows a **Choose File** picker (`loanRef,borrowerName,phone,address,principal,overdue,dpd,[branchId]`) | 200 `{imported, failed}` |
| ⛔ CSV bad rows | file with a negative principal / short phone | 200 with `failed>0` (row-level) |

### 2.4 Collateral assets 🔗 (needs an accountId)
- ✅ `POST /api/collateral-assets` **[ADMIN, FIELD_OFFICER]**
  ```json
  { "accountId":"<ACC>", "assetType":"PROPERTY", "description":"2BHK flat", "estimatedValue":4500000 }
  ```
  → 201 `{ "assetId":"COL-2026-000001", "verificationStatus":"UNVERIFIED" }`
  ⛔ `estimatedValue:-1` → 400 · ⛔ `assetType:"LAND"` → 400 bad enum · ⛔ AGENT token → 403
- ✅ `GET /api/collateral-assets/account/{accountId}` **[ADMIN, FIELD_OFFICER, PM]** → 200 List.
- ✅ `GET /api/collateral-assets/{id}` → 200 · ⛔ missing → 404.
- ✅ `PUT /api/collateral-assets/{id}` **[ADMIN, FIELD_OFFICER]** → 200.

### 2.5 Allocation rules — **[ADMIN, COLLECTIONS_AGENT, PORTFOLIO_MANAGER]**
- ✅ `POST /api/allocations` (rule-driven engine — fields: `strategy`(ROUND_ROBIN|LEAST_LOADED|BRANCH_BASED),
  `bucket?`, `targetRole`, `daysInBucketThreshold?`, `minDpd?`, `gracePeriodDays?`, `capacityLimit?`, `branchId?`, `priority`)
  ```json
  { "name":"NPA Legal Escalation", "strategy":"LEAST_LOADED", "bucket":"NPA", "targetRole":"LEGAL_OFFICER", "daysInBucketThreshold":15, "capacityLimit":50, "priority":100, "active":true }
  ```
  → 201 `{ "ruleId":"ALR-2026-000001" }`
  ⛔ missing `name`/`strategy`/`targetRole` → 400 · ⛔ `strategy:"RANDOM"` → 400 bad enum · ⛔ negative `capacityLimit`/`gracePeriodDays` → 400
- ✅ `GET /api/allocations?page=0&size=20` (priority desc), `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (204).
- ✅ `POST /api/allocations/execute` → 200 `{ "assigned": N }` (assigns unassigned ACTIVE accounts via the strategy).
- ✅ **rule-driven escalation** — trigger it **on demand** with `POST /api/allocations/escalate` (ADMIN/PM), which
  runs the exact same engine as the nightly `EscalationScheduler` (no need to wait for the 02:00 cron):
  1. `PUT /api/accounts/{id}` `{ "dpd":200, "daysInCurrentBucket":20 }` → account is NPA, stagnant.
  2. create the NPA rule above (targetRole LEGAL_OFFICER, daysInBucketThreshold 15).
  3. `POST /api/allocations/escalate` → `{ "reassigned": 1 }`; `GET /api/accounts/{id}` shows `assignedAgentId` = a LEGAL_OFFICER.
  An NPA account with `daysInCurrentBucket ≥ 15` and **no active PTP** → reassigned to a LEGAL_OFFICER by the chosen strategy.
  ⛔ account with an **active PTP** → not disturbed. ✅ re-run → **idempotent** (already-correctly-assigned accounts skipped).
  ✅ set a `capacityLimit` and fill a target to capacity → the engine skips that candidate; ✅ `gracePeriodDays`/`minDpd`
  hold back accounts below the DPD threshold; ✅ `BRANCH_BASED` only targets staff in the account's branch; ✅ higher `priority` rule wins when two match.

---

## 3. MODULE: Contact & Follow-Up (contact-service) 🔗 needs accountIds

### 3.1 Contact attempts
- ✅ `POST /api/contacts` **[ADMIN, PM, COLLECTIONS_AGENT]** (as agent, `agentId` auto-inferred)
  ```json
  { "accountId":"<ACC>", "channel":"CALL", "outcome":"CONNECTED", "notes":"Promised to pay by month end" }
  ```
  → 201 `{ "contactId":"CON-2026-000001", "agentId":"<caller>", "status":"LOGGED" }`
  ⛔ missing `accountId` → 400 · ⛔ `channel:"WHATSAPP"` → 400 bad enum · ⛔ `outcome` missing → 400 · ⛔ FIELD token → 403
- ✅ `GET /api/contacts?accountId=<ACC>&page=0&size=20` → 200 Page · ✅ `GET /api/contacts/{id}` → 200 · ⛔ missing → 404.
- ✅ `PUT /api/contacts/{id}` **[ADMIN, PM]** → 200 · ⛔ AGENT token → 403.

### 3.2 Promise-to-Pay (PTP) lifecycle — **[ADMIN, COLLECTIONS_AGENT]**
- ✅ create `POST /api/ptp`
  ```json
  { "accountId":"<ACC>", "ptpDate":"2026-07-17", "ptpAmount":15000, "commitmentDate":"2026-07-31" }
  ```
  → 201 `{ "ptpId":"PTP-2026-000001", "status":"ACTIVE" }`
  ⛔ `ptpAmount:0` → 400 · ⛔ missing `commitmentDate` → 400
- ✅ list `GET /api/ptp?accountId=<ACC>` · ✅ `GET /api/ptp/{id}`.
- ✅ **partial** `PATCH /api/ptp/{id}/payment?actualPaidAmount=5000` → 200 `status=PARTIAL`.
- ✅ **kept** `PATCH /api/ptp/{id}/payment?actualPaidAmount=15000` → 200 `status=KEPT`.
- ✅ reschedule `PATCH /api/ptp/{id}/reschedule?commitmentDate=2026-08-15` → 200 `status=RESCHEDULED`.
- ⛔ payment on missing id → 404 · ⛔ bad date format → 400.

### 3.3 Borrower contacts — **[ADMIN, COLLECTIONS_AGENT]** (+PM read)
- ✅ `POST /api/borrower-contacts`
  ```json
  { "accountId":"<ACC>", "contactType":"PRIMARY", "name":"Anil Sharma", "phone":"9800000001", "relationship":"Self" }
  ```
  → 201 `{ "contactRecordId":"BOR-2026-000001" }`
  ⛔ `phone:"12345"` → 400 `fieldErrors.phone` · ⛔ missing `name` → 400 · ⛔ `contactType:"FRIEND"` → 400 bad enum
- ✅ `GET /api/borrower-contacts`, `GET /account/{accountId}`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (204).

---

## 4. MODULE: Field Recovery (field-service) 🔗

### 4.1 Field visits — **[ADMIN, FIELD_OFFICER, PORTFOLIO_MANAGER]**
- ✅ schedule `POST /api/visits`
  ```json
  { "accountId":"<ACC>", "officerId":"<FIELD_USER_ID>", "scheduledDate":"2026-07-25", "nextActionRequired":"Verify collateral" }
  ```
  → 201 `{ "visitId":"VIS-2026-000001", "status":"SCHEDULED" }`
  ⛔ `scheduledDate:"2020-01-01"` (past) → 400 `"Scheduled date cannot be in the past"` · ⛔ missing `officerId` → 400
  ⛔ `officerId` of a **non-existent / inactive** user → 400 · ⛔ `officerId` of a user whose role isn't FIELD_OFFICER → 400 `NOT_A_FIELD_OFFICER`
- ✅ list `GET /api/visits?accountId=<ACC>` · ✅ `GET /api/visits/my-visits` (as field officer → own visits).
- ✅ complete `PATCH /api/visits/{id}/complete` `{ "visitDate":"2026-07-25", "borrowerMet":true, "assetSighted":true, "outcomeSummary":"Met borrower", "nextActionRequired":"None" }` → 200 `status=COMPLETED` **(only as the officer the visit is assigned to)**.
- ✅ missed `PATCH /api/visits/{id}/missed` → 200 `status=MISSED`.
- ⛔ **IDOR**: `field@dp.com` (officer B) completing/missing a visit assigned to officer A → **403** (object-level authz). ✅ ADMIN / PORTFOLIO_MANAGER may modify any visit.
- ⛔ complete/missed on missing id → 404.

### 4.2 Asset verification — **[ADMIN, FIELD_OFFICER]** (+PM read) 🔗 needs visitId + assetId
- ✅ `POST /api/asset-verifications`
  ```json
  { "visitId":"<VIS>", "assetId":"<COL>", "condition":"GOOD", "currentLocation":"On site", "realisableValue":4200000, "remarks":"OK", "verificationDate":"2026-07-25" }
  ```
  → 201 `{ "reportId":"AVR-2026-000001" }`; **side-effect:** the collateral asset flips to `VERIFIED`
  (re-`GET /api/collateral-assets/{assetId}` to confirm).
  ⛔ `realisableValue:-1` → 400 · ⛔ `condition:"BROKEN"` → 400 bad enum · ⛔ missing `assetId` → 400 · ⛔ AGENT token → 403
- ✅ `GET /api/asset-verifications?page=0&size=20`, `GET /visit/{visitId}`, `GET /{id}`, `PUT /{id}`.

---

## 5. MODULE: Settlement & Restructuring (settlement-service) 🔗

### 5.1 Settlement lifecycle — approval chain is DERIVED from the haircut (client never sends a level)
Matrix: `<10%`→[L1]; `10–25%`→[L1,L2]; `≥25%`→[L1,L2,L3]. Approvals are **sequential** — each level approves in order.
1. ✅ create `POST /api/settlements` **[ADMIN, SETTLEMENT_OFFICER]** (as `so@dp.com`)
   ```json
   { "accountId":"<ACC>", "totalOutstanding":150000, "settlementAmount":105000, "paymentDeadline":"2026-08-15", "notes":"One-time settlement" }
   ```
   → 201 `{ "proposalId":"SET-2026-000001", "haircutPercent":30.0, "requiredApprovalChain":["L1","L2","L3"], "currentStep":null, "status":"DRAFT" }`
   ⛔ `settlementAmount:0` → 400 · ⛔ `settlementAmount>totalOutstanding` → 400 business rule (`ruleCode`). (There is no `approvalLevel` field — sending one is simply ignored.)
2. ✅ submit `PATCH /api/settlements/{id}/submit` → 200 `status=PENDING_APPROVAL`, `currentStep="L1"` (always starts at L1); L1 approver notified.
3. ✅ decide L1 (as `l1@dp.com`) `POST /api/settlements/{id}/decide?level=L1` body `{ "decision":"APPROVE", "comments":"L1 ok" }`
   → 200; for a 30% haircut it **advances**: `status=PENDING_APPROVAL`, `currentStep="L2"` (NOT approved yet).
   `level` is a **query parameter** (an enum → Swagger renders it as a dropdown) and must match the pending step:
   ⛔ `?level=L1` when `currentStep="L2"` → **400 `APPROVAL_LEVEL_MISMATCH`** ("You selected L1 but this settlement is awaiting L2 approval").
   ⛔ `l1@dp.com` decides an L2 step (`?level=L2`, wrong role) → 403.
   ⛔ the raising officer decides → 403 maker-checker. ⛔ `decision:"MAYBE"` → 400 bad enum · ⛔ missing `level` param → 400 · ⛔ `comments`>1000 → 400.
4. ✅ decide L2 (as `l2@dp.com`) APPROVE → `currentStep="L3"`; then L3 (as `l3@dp.com`) APPROVE → `status=APPROVED`, `currentStep=null`.
   ✅ Reject at any step (as that step's approver) → `status=REJECTED`; officer notified.
5. ✅ mark paid `PATCH /api/settlements/{id}/mark-paid` (APPROVED only) → 200 `status=PAID`. ⛔ on non-APPROVED → 400.
6. ✅ queries: `GET /api/settlements?page=0&size=20`, `/outstanding`, `/past-deadline`, `/approval-queue`, `/{id}`, `PUT /{id}` (DRAFT only).

### 5.2 Restructuring
- ✅ create `POST /api/restructuring` **[ADMIN, SETTLEMENT_OFFICER]**
  ```json
  { "accountId":"<ACC>", "revisedTenure":24, "revisedEmi":8500, "waiverAmount":10000, "startDate":"2026-08-01" }
  ```
  → 201 `{ "restructureId":"RST-2026-000001", "status":"DRAFT" }`
  ⛔ `revisedTenure:0` / `revisedEmi:-1` → 400 · ⛔ missing `startDate` → 400
- ✅ `GET /api/restructuring` (broad read roles incl. approvers), `GET /{id}`, `GET /account/{accountId}`, `PUT /{id}`.
- ✅ approve `PATCH /api/restructuring/{id}/approve` **[ADMIN, L1/L2/L3_APPROVER]** → 200 `status=APPROVED`.
- ✅ reject `PATCH /api/restructuring/{id}/reject` → 200.
- ⛔ approve with SETTLEMENT_OFFICER token → 403 (maker cannot approve).

---

## 6. MODULE: Legal Proceedings (legal-service) — class **[ADMIN, LEGAL_OFFICER, PORTFOLIO_MANAGER]** 🔗

- ✅ case `POST /api/legal/cases` **[ADMIN, LEGAL_OFFICER]**
  ```json
  { "accountId":"<ACC>", "caseType":"SARFAESI_ACTION", "filingDate":"2026-07-10", "courtName":"DRT Mumbai", "caseNumber":"DRT/2026/114" }
  ```
  → 201 `{ "caseId":"LEG-2026-000001", "status":"FILED", "legalOfficerId":"<caller>" }`
  ⛔ `caseType:"FOO"` → 400 · ⛔ missing `courtName`/`caseNumber` → 400 · ⛔ AGENT token → 403
- ✅ `GET /api/legal/cases?page=0&size=20`, `GET /cases/{id}`, `PUT /cases/{id}`.
- ✅ hearing `POST /api/legal/hearings` 🔗
  ```json
  { "caseId":"<LEG>", "hearingDate":"2026-08-05", "hearingOutcome":"ADJOURNED", "nextHearingDate":"2026-09-05", "notes":"Adjourned" }
  ```
  → 201 `{ "hearingId":"HRG-2026-000001" }` · ✅ `GET /api/legal/cases/{caseId}/hearings` → List.
  ⛔ `hearingOutcome:"WON"` → 400 bad enum.
- ✅ order `POST /api/legal/orders` 🔗
  ```json
  { "caseId":"<LEG>", "orderType":"ATTACHMENT_ORDER", "issuedDate":"2026-08-10", "executionDeadline":"2026-09-10" }
  ```
  → 201 `{ "orderId":"ORD-2026-000001" }` · ✅ `GET /orders`, `GET /orders/{id}`, `DELETE /orders/{id}` (204).
  ⛔ missing `executionDeadline` → 400.

---

## 7. MODULE: Notifications (notification-service) — the caller's own inbox

Notifications are raised by other flows (PTP breach, settlement expiry/decision, legal alerts). After running
§3–§6 (and the schedulers), log in as the relevant officer and:
- ✅ `GET /api/notifications?page=0&size=20` → 200 Page of the caller's notifications.
- ✅ `GET /api/notifications/unread-count` → `{ "unreadCount": N }`.
- ✅ `GET /api/notifications/{id}` → 200 · ⛔ another user's id → 404 (scoped to caller).
- ✅ `PATCH /api/notifications/{id}/read` → 200 `status=READ` · ✅ `/dismiss` → `DISMISSED`.
- ✅ `PATCH /api/notifications/read-all` → `{ "updated": N }`.

---

## 8. MODULE: Analytics (analytics-service) — **[ADMIN, PORTFOLIO_MANAGER]**

Run **after** seeding §2–§6 (aggregates over Feign from every service). As `pm@dp.com`:
- ✅ `GET /api/analytics/dashboard` → consolidated totals (accounts, overdue, cash, recovery rate, PTP breach, settlements, legal conversion, field-visit success).
- ✅ `GET /api/analytics/bucket-distribution` → counts per bucket (matches §2 data).
- ✅ `/ptp-metrics`, `/settlement-metrics`, `/recovery-rate`, `/bucket-migration`, `/cash-collected`, `/field-visit-success`, `/legal-conversion` → 200 maps.
- ✅ `POST /api/analytics/reports/generate?scope=Branch` → 201 `{ "reportId":"RPT-2026-000001" }`.
- ✅ `GET /api/analytics/reports?page=0&size=20` → 200 Page.
- ⛔ AGENT token on any → 403.
- **Resilience:** stop one source service, call `/dashboard` again → still 200 with that source's metrics as 0 (circuit-breaker fallback), not 500.

---

## 9. Cross-module & simultaneous tests

### 9.1 Sequential end-to-end (one happy borrower journey)
1. ADMIN registers an agent + field officer (§1.2). 2. Agent creates account (§2.1) → auto-assigned.
3. Agent logs a contact + PTP (§3). 4. Field officer schedules a visit, adds collateral, verifies it (§2.4 + §4).
5. Settlement officer raises + submits a settlement; approver approves; mark paid (§5). 6. Legal officer files a
case + hearing + order (§6). 7. PM checks analytics reflect all of the above (§8). 8. ADMIN checks audit log
has every create/update with the correct `userId` (§1.5).

### 9.2 Security / isolation (run in parallel)
- Fire the **same** protected endpoint concurrently with different role tokens → each sees only what its role
  allows (403s where expected); no token bleed between requests.
- ⛔ Send a request with a **spoofed** `X-Auth-Role: ADMIN` header + an AGENT token → gateway strips it; still treated as AGENT (403 on ADMIN-only ops).
- ⛔ Expired token (wait >30 min or tamper) → 401 on every protected call.
- ✅ Browser preflight: `OPTIONS /api/auth/login` with `Origin: http://localhost:4200` → 200, CORS headers, no auth required.

### 9.3 ID uniqueness under concurrency
- Fire **50 parallel** `POST /api/accounts` (e.g. `seq 50 | xargs -P20 -I{} curl ...`) → all 201, 50 **distinct**
  `ACC-2026-0000..` ids, no duplicates/gaps beyond rolled-back inserts; `SELECT next_val FROM debtpulse_account.id_sequence` equals the count.
- Repeat for a second entity in parallel (e.g. PTPs) → independent sequences, no cross-contamination.

### 9.4 Maker-checker concurrency (settlement)
- Two approvers hit `POST /api/settlements/{id}/decide` for the same proposal simultaneously → exactly one
  wins (APPROVED/REJECTED); the second gets a business-rule 400 (already decided), never a double transition.

### 9.5 Validation is server-side (bypass the UI)
- Re-run the ⛔ cases from §1.2 / §2.1 / §3.3 directly via `curl`/Postman (no frontend) → all still rejected with
  400 + `fieldErrors`. Confirms the backend is the final authority.

---

## 10. Schedulers (background jobs)

Either wait for the cron time or temporarily set the cron to `"0 */2 * * * ?"` in `config-repo` to trigger fast.
| Job | Setup | Expected |
|-----|-------|----------|
| PTP breach (contact, 08:00) | create a PTP with `commitmentDate` = yesterday, status ACTIVE | job flips it to `BROKEN`; a `PTP` notification appears for the agent |
| Settlement expiry (settlement, 01:00) | APPROVED settlement with past `paymentDeadline` | → `EXPIRED`; `SETTLEMENT` notification to officer; audit `SETTLEMENT_EXPIRED` by `SYSTEM` |
| DPD ageing (account, 00:30) | any ACTIVE account | `dpd` +1, bucket re-derived, `daysInCurrentBucket` updated |
| Hearing alert / visit reminder / escalation | upcoming hearing / visit / bucket breach | notification raised to the owner |

Verify jobs logged start/finish in `logs/spring.log` and that a downstream outage during a job only drops the
notification (logged warning) without rolling back the primary state change.

---

## 11. Observability

- ✅ **Correlation id** — call any endpoint with `-H "X-Correlation-Id: trace-abc"`; the response echoes it and
  every `logs/spring.log` line for that request shows `[trace-abc]`. Omit the header → one is generated and
  still echoed. Make a call that fans out over Feign (e.g. `GET /api/analytics/dashboard`) → the **same** id
  appears in the downstream services' logs.
- ✅ **Audit trail** — log in / refresh / log out, then grep `logs/spring.log` for `AUDIT action=LOGIN` /
  `TOKEN_REFRESH` / `LOGOUT` — each carries `user=`, `entity=User`, `outcome=`, `ip=`, `correlationId=`.
- ✅ **Health & probes** (per service, e.g. auth on 8081):
  - `GET /actuator/health` → `{"status":"UP", ...}` with component details (db, diskSpace, circuitBreakers).
  - `GET /actuator/health/liveness` → `UP`; `GET /actuator/health/readiness` → `UP` (turns `OUT_OF_SERVICE` if the DB is down).
  - `GET /actuator/metrics`, `/actuator/circuitbreakers`, `/actuator/info` → 200.
  - `GET /actuator/prometheus` → 200 **once** `micrometer-registry-prometheus` is added (online-only jar; listed in exposure already).

---

## Appendix — HTTP status quick map
`200` ok · `201` created (POST) · `204` deleted · `400` validation/bad-enum/business-rule/malformed ·
`401` no/instr/expired JWT · `403` wrong role · `404` not found · `405` wrong method · `409` conflict · `415` bad content-type · `500` unexpected (logged).
Error body is always the standard envelope: `{ timestamp, status, error, message, path, [fieldErrors], [ruleCode] }`.
