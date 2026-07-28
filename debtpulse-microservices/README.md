# DebtPulse — Microservices Edition

Debt Recovery & Collections Management System, re-architected from the original Spring Boot
monolith into a Spring Cloud microservices platform. All original REST APIs are preserved,
with the same role-based access control, now split across independently deployable services.

## Architecture

```
                         ┌──────────────────────────┐
   client / Swagger ───▶ │  API Gateway  (9090)     │  JWT GlobalFilter + Swagger aggregation
                         └───────────┬──────────────┘
                                     │ lb:// (Eureka)  + X-Auth-* identity headers
   ┌───────────┬───────────┬─────────┼─────────┬───────────┬───────────┬─────────────┐
   ▼           ▼           ▼         ▼         ▼           ▼           ▼             ▼
 auth       account     contact    field    settlement   legal     analytics   notification
 (8081)     (8082)      (8083)    (8084)     (8085)      (8086)     (8087)       (8088)
   │            │           │         │          │          │          │            │
   └── each owns its own MySQL schema; cross-service reads go over Feign + Resilience4j ──┘

 Supporting infra:  Config Server (8888)  •  Eureka registry (8761)
```

### Modules
| Module | Port | Module ref | Owns (schema) |
|--------|------|-----------|---------------|
| config-server | 8888 | — | serves `config-repo/` |
| eureka-server | 8761 | — | service registry |
| api-gateway | 9090 | — | routing, JWT validation, Swagger aggregation |
| common-lib | — | shared | security filter, Feign interceptor, AOP logger, shared enums, DTOs |
| auth-service | 8081 | 2.1 IAM | `debtpulse_auth` (users, audit_log) |
| account-service | 8082 | 2.2 Portfolio | `debtpulse_account` (delinquent_account, collateral_asset, allocation_rule) |
| contact-service | 8083 | 2.3 Contact | `debtpulse_contact` (contact_attempt, promise_to_pay, borrower_contact) |
| field-service | 8084 | 2.4 Field Recovery | `debtpulse_field` (field_visit, asset_verification_report) |
| settlement-service | 8085 | 2.5 Settlement | `debtpulse_settlement` (settlement_proposal, approval_step, restructuring_proposal) |
| legal-service | 8086 | 2.6 Legal | `debtpulse_legal` (legal_case, court_hearing, recovery_order) |
| analytics-service | 8087 | 2.7 Analytics | `debtpulse_analytics` (recovery_report) |
| notification-service | 8088 | 2.8 Notifications | `debtpulse_notification` (notification) |

## Security model
- **auth-service** issues the JWT on login (30-min expiry, HS256). Claims: `sub`=userId, `role`, `branchId`, `name`.
- **api-gateway** runs a `GlobalFilter` that validates the JWT on every protected request and forwards
  the identity as trusted headers (`X-Auth-UserId`, `X-Auth-Role`, `X-Auth-BranchId`, `X-Auth-Name`).
  It strips any client-supplied copies of those headers (anti-spoofing).
- Each service rebuilds its Spring Security context from those headers via the shared
  `RoleBasedHeaderFilter`, so method-level `@PreAuthorize("hasRole('…')")` works exactly as in the monolith.
- Inter-service Feign calls propagate the same headers via `FeignClientInterceptor` (scheduled jobs call as `SYSTEM`/`ADMIN`).

### Roles
`COLLECTIONS_AGENT, FIELD_OFFICER, LEGAL_OFFICER, SETTLEMENT_OFFICER, L1_APPROVER, L2_APPROVER, L3_APPROVER, PORTFOLIO_MANAGER, ADMIN`

### Seed users (all password `password`)
`admin@dp.com` (ADMIN), `agent@dp.com` (COLLECTIONS_AGENT), `field@dp.com` (FIELD_OFFICER),
`legal@dp.com` (LEGAL_OFFICER), `so@dp.com` (SETTLEMENT_OFFICER), `l1/l2/l3@dp.com` (approvers),
`pm@dp.com` (PORTFOLIO_MANAGER). Seeded automatically by auth-service on first start.

## Prerequisites
- Java 21, Maven 3.9+
- MySQL on `localhost:3306`, user `root` / password `root` (schemas auto-created via `createDatabaseIfNotExist`).
  Adjust credentials in `config-repo/application.yml` if different.

## Build
```bash
./build.sh                 # or: mvn clean install
```
> On a corporate network that intercepts TLS to Maven Central you may hit PKIX errors on the first
> build. Use the wagon transport with relaxed SSL (already wired into `build.sh`):
> ```
> mvn -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true clean install
> ```

## Run (order matters)
1. `config-server` (8888) → 2. `eureka-server` (8761) → 3. `api-gateway` (9090) → 4. all business services.

Windows: `start-all.bat`. Or per module: `cd <module> && mvn spring-boot:run`.

## Frontend (React + Bootstrap) — JavaScript **and** TypeScript editions
A professional, banking-grade SPA is provided in **two interchangeable editions** (pick whichever the
evaluation needs) — both cover every module (login/password, dashboard, portfolio, contact/PTP, field,
settlement, legal, analytics, notifications, admin console) with role-based navigation matching the gateway RBAC:
- **`frontend/`** — React 18 + Vite + React-Bootstrap (JavaScript) — dev port **5173**
- **`frontend-ts/`** — the same app in **TypeScript** (strict, fully typed) — dev port **5174**
```bash
cd frontend    && npm install && npm run dev    # http://localhost:5173
cd frontend-ts && npm install && npm run dev    # http://localhost:5174
```
Both proxy `/api` → gateway `:9090`, and each ships a `Dockerfile` + `nginx.conf` (multi-stage build →
nginx serving `dist/`, proxying `/api` to `api-gateway:9090`). See each folder's `README.md` for the
screen-to-API map.

## Explore
- Eureka dashboard: http://localhost:8761
- **Aggregated Swagger UI (all services): http://localhost:9090/swagger-ui.html** (pick a service from the dropdown)
- Each service also serves its own Swagger at `http://localhost:<port>/swagger-ui.html`.

## Cross-cutting requirements coverage
- **Auth/authz**: Spring Security + JWT (gateway validates, services enforce roles). ✔
- **Discovery**: Eureka. ✔  **Gateway**: Spring Cloud Gateway with JWT `GlobalFilter`. ✔
- **Config server**: native, backed by local `config-repo/`. ✔
- **Feign** inter-service comms + **Resilience4j** circuit breakers with fallbacks. ✔
- **Layering** per service: entity / dto (validated) / service (interface+impl) / config (security + Feign
  interceptor + role header filter) / exception (own `GlobalExceptionHandler` + exceptions) / repository /
  mapper / controller. Domain enums are shared from `common-lib` (`com.debtpulse.common.enums`). ✔
- **Repositories**: `PagingAndSortingRepository`+`CrudRepository` by default; `JpaRepository`+
  `JpaSpecificationExecutor` only where dynamic filtering is required. ✔
- **AOP logging** to `logs/spring.log` via `LoggingAspect`. ✔
- **Business IDs**: every entity `@Id` uses the shared `@BusinessId` generator → sortable, human-readable
  `PREFIX-YEAR-NNNNNN` ids (e.g. `ACC-2026-000001`), allocated atomically per service from `id_sequence`. ✔
- **Tests**: JUnit 5 + Mockito for services and controllers. ✔

See `BUILD_CONVENTIONS.md` and `INTERNAL_CONTRACTS.md` for the engineering standards and the
service-to-service Feign contracts. See **`API_GUIDE.md`** for the end-to-end request/response flow,
validation rules, the full test-case matrix, and a ready-to-post ~20-record sample dataset. See
**`TESTING_GUIDE.md`** for a detailed, per-endpoint test script (every API, happy + negative cases,
sequenced module-by-module flow, and cross-module / concurrent tests).
