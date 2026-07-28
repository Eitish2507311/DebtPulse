# DebtPulse — Inter-Service Feign Contracts (CANONICAL — match paths & JSON field names EXACTLY)

Every provider endpoint below lives at `/api/internal/**` (NOT routed by the gateway; reached
service-to-service via Feign on `lb://<service>`). Consumers must define a matching `@FeignClient`
whose paths and DTO field names are byte-for-byte compatible. JSON field names = the record component
names shown here.

---
## auth-service  (name: `auth-service`)  — ALREADY BUILT
Provider endpoints under base `/api/internal`:
- `GET  /users/{id}` → `UserDto`
- `GET  /users/{id}/exists` → `boolean`
- `GET  /users/by-role/{role}/first` → `UserDto` (may be null-body)
- `GET  /users/by-role/{role}` → `List<UserDto>`
- `GET  /users/by-role/{role}/active?branchId=` → `List<UserDto>`
- `POST /audit-logs` (body `AuditLogRequest`) → 201

`UserDto` (record): `userId, fullName, email, phone, role, branchId, status, createdAt(LocalDateTime)`
`AuditLogRequest` (record): `userId, action, entityType, recordId, sourceService`
`role` path var accepts the role NAME string (e.g. `COLLECTIONS_AGENT`, `L1_APPROVER`, `PORTFOLIO_MANAGER`).

Consumer client (copy into each service that needs users/audit):
```java
@FeignClient(name="auth-service", path="/api/internal", fallback=AuthClientFallback.class)
public interface AuthClient {
  @GetMapping("/users/{id}") UserDto getUser(@PathVariable String id);
  @GetMapping("/users/{id}/exists") boolean userExists(@PathVariable String id);
  @GetMapping("/users/by-role/{role}/first") UserDto firstByRole(@PathVariable String role);
  @GetMapping("/users/by-role/{role}/active") java.util.List<UserDto> activeByRole(@PathVariable String role, @RequestParam(required=false) String branchId);
  @PostMapping("/audit-logs") void audit(@RequestBody AuditLogRequest req);
}
```
Every service should record audits for create/update/delete/decision actions via `AuthClient.audit(...)`
with `sourceService = "<this-service>"` and `userId = AuthContext.currentUserId()`.

---
## account-service  (name: `account-service`)
Provider endpoints under base `/api/internal`:
- `GET   /accounts/{id}` → `AccountDto` (404 if missing)
- `GET   /accounts/{id}/exists` → `boolean`
- `PATCH /accounts/{id}/status?status={AccountStatus}` → 200 — lifecycle cascade (DP5-18/20). settlement-service sets `SETTLED` when a settlement is paid; legal-service sets `LEGAL` when a case is filed. Callers treat this as best-effort (circuit-breaker fallback absorbs outages; a failure must not roll back the caller's own transaction).
- `POST  /collateral-assets/{assetId}/mark-verified` → 200 (set VerificationStatus=VERIFIED, lastVerifiedDate=now)
- `GET  /accounts/stats` → `Map<String,Object>` keys: `totalAccounts, activeAccounts, settledAccounts, legalAccounts, writeOffAccounts, totalOverdue(number), byBucket(Map bucket->count)`

`AccountDto` (record): `accountId, loanRef, borrowerName, phone, address, branchId, principalAmount(BigDecimal), totalOverdue(BigDecimal), dpd(Integer), bucket(String), status(String), assignedAgentId(String)`

---
## contact-service  (name: `contact-service`)
Provider endpoints under base `/api/internal`:
- `GET /ptp/active-count?accountId=` → `long` (count of PromiseToPay with status ACTIVE for that account)
- `GET /ptp/stats` → `Map<String,Object>` keys: `totalPtp, activePtp, keptPtp, brokenPtp, ptpBreachRate(number)`
- `GET /contacts/stats` → `Map<String,Object>` keys: `totalContacts, connectedContacts`

---
## field-service  (name: `field-service`)
Provider endpoints under base `/api/internal`:
- `GET /visits/stats` → `Map<String,Object>` keys: `totalVisits, completedVisits, missedVisits, fieldVisitSuccessRate(number)`

---
## settlement-service  (name: `settlement-service`)
Provider endpoints under base `/api/internal`:
- `GET /settlements/stats` → `Map<String,Object>` keys: `totalSettlements, approvedSettlements, rejectedSettlements, paidSettlements, pendingSettlements`

---
## legal-service  (name: `legal-service`)
Provider endpoints under base `/api/internal`:
- `GET /legal/stats` → `Map<String,Object>` keys: `totalCases, filedCases, decreedCases, settledCases, legalConversionRate(number)`

---
## notification-service  (name: `notification-service`)
Provider endpoints under base `/api/internal`:
- `POST /notifications` (body `NotificationRequest`) → `NotificationDto` (201)

`NotificationRequest` (record): `userId, message, category(String enum name)`
Categories (enum `NotifCategory` NAMES — send these exact strings):
`PTP, FIELD_VISIT, SETTLEMENT, LEGAL, ESCALATION, PORTFOLIO`.

Consumer client (copy into contact/field/settlement/legal):
```java
@FeignClient(name="notification-service", path="/api/internal", fallback=NotificationClientFallback.class)
public interface NotificationClient {
  @PostMapping("/notifications") void notify(@RequestBody NotificationRequest req);
}
```
Each consumer defines its own local `NotificationRequest` record with fields `userId, message, category`.
