# DebtPulse — Frontend (React + Bootstrap)

Professional, banking-grade single-page app for the DebtPulse collections platform. Talks to the
Spring Cloud **API Gateway** (`http://localhost:9090`) and mirrors its RBAC across all nine roles.

## Stack
- **React 18** + **Vite** (fast dev server, production build)
- **React Router 6** (route-level RBAC guards)
- **React-Bootstrap 5** + **Bootstrap Icons** + a custom banking theme (`src/styles/theme.css`)
- **Axios** (JWT interceptor, centralised error handling) · **Recharts** (analytics)

## Architecture
```
src/
  api/         client.js (axios + interceptors), services.js (every gateway endpoint)
  auth/        AuthContext, ProtectedRoute, roles.js (RBAC map)
  components/  Layout (sidebar+topbar), DataTable, FormModal, Field, ConfirmDialog, ToastHost, ui.jsx
  hooks/       usePaged / useAsync
  pages/       login, password reset, dashboard, portfolio, contact, field,
               settlement, legal, analytics, notifications, admin (users + audit)
  utils/       enums.js (status→colour), format.js (₹ / dates / %)
```

### How it maps to the modules (Jira DP5-24…40)
| Screen | Route | Roles | Backend |
|--------|-------|-------|---------|
| Login / Forgot / Reset / Change password | `/login`, `/forgot-password`, `/reset-password`, `/change-password` | public / self | auth-service |
| Dashboard | `/dashboard` | all (KPIs for ADMIN/PM) | analytics + notifications |
| Portfolio + Account detail (collateral, borrower contacts) | `/portfolio`, `/portfolio/:id` | ADMIN, AGENT, PM | account-service |
| Allocation Rules | `/admin/allocations` | ADMIN, AGENT, PM | account-service |
| Contact & PTP & Borrower contacts | `/contacts` | ADMIN, AGENT, PM | contact-service |
| Field visits + Asset verification | `/field` | ADMIN, FIELD, PM | field-service |
| Settlements + Restructuring | `/settlements` | ADMIN, SO, approvers, PM | settlement-service |
| Legal cases + Hearings + Orders | `/legal`, `/legal/:id` | ADMIN, LEGAL, PM | legal-service |
| Analytics | `/analytics` | ADMIN, PM | analytics-service |
| Notifications | `/notifications` | all | notification-service |
| User Management + Audit Trail | `/admin/users`, `/admin/audit` | ADMIN (audit: +PM) | auth-service |

The sidebar auto-hides menu items the signed-in role can't access, and each route is also guarded
server-side — invalid role → the app shows a 403 page, and any API 403 surfaces as a toast.

## Run (dev)
```bash
cd frontend
npm install
npm run dev          # http://localhost:5173  (Vite proxies /api → gateway :9090)
```
Start the backend first (config-server → eureka → gateway → services). Then sign in with any seed
user, e.g. `admin@dp.com` / `password`.

- `VITE_API_TARGET` (in `.env`) points the dev proxy at the gateway (default `http://localhost:9090`).
- `VITE_API_BASE` is the axios base (`/api` by default, so the proxy handles CORS-free local calls).

## Build (production)
```bash
npm run build        # outputs dist/  (serve behind the same gateway/ingress as /)
npm run preview      # preview the production build locally
```

## Notes
- **Validation** mirrors the backend: forms show field-level errors returned in the standard
  `ErrorResponse.fieldErrors` envelope (strong password, 10-digit phone, `@dp.com` email, etc.).
- **Auth**: JWT is stored in `localStorage`, attached to every request; any 401 clears the session and
  redirects to login. 30-minute token expiry is handled transparently.
- **Business IDs** (`ACC-2026-000001`, …) are shown verbatim so they're easy to reference across screens.
