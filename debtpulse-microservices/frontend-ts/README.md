# DebtPulse — Frontend (React + **TypeScript** + Bootstrap)

The **TypeScript** edition of the DebtPulse SPA. Functionally identical to `../frontend` (the JavaScript
edition) — same screens, same banking theme, same RBAC and API contracts — but fully typed end to end.
Pick whichever edition your evaluation requires.

## What's different from the JS edition
- **Strict TypeScript** (`strict: true`) — `npm run build` runs `tsc --noEmit` before Vite, so a type error
  fails the build.
- **`src/types.ts`** — every backend DTO, the `PageResponse<T>` / `AppError` envelopes, table `Column<T>`,
  and the session/role types.
- **Typed API layer** — `services.ts` returns `AxiosResponse<Account>`, `PageResponse<Ptp>`, etc., so pages
  get autocompletion and compile-time safety on every field.
- **Typed hooks & components** — `usePaged<T>()`, `useAsync<T>()`, generic `<DataTable<T>>`, typed context.
- Dynamic form state (`FormValues`) is intentionally loose (heterogeneous fields); the API and domain models
  stay strongly typed at the call site.

Everything else (folder layout, theme, screen-to-API map, Docker/nginx) matches the JS edition — see
`../frontend/README.md` for the full screen/route/role table.

## Run (dev) — port 5174 (so it can run alongside the JS edition on 5173)
```bash
cd frontend-ts
npm install
npm run dev            # http://localhost:5174  (Vite proxies /api → gateway :9090)
npm run typecheck      # tsc --noEmit only
```

## Build / Docker
```bash
npm run build          # tsc --noEmit && vite build  → dist/
docker build -t debtpulse-ui-ts .     # multi-stage node build → nginx serve (proxies /api → api-gateway:9090)
```

Sign in with any seed user, e.g. `admin@dp.com` / `password`.
