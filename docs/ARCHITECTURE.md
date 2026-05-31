# Chores — Architecture

```
                      Tailnet (Tailscale)
                              │
                              ▼
              ┌─────────────────────────────┐
              │  Ingress (Tailscale class)  │
              │ chores.stoat-perch.ts.net   │
              └────────────┬────────────────┘
                           │ HTTPS
                ┌──────────┴──────────────────────┐
                │ path-based routing               │
                │  /api/*       → backend          │
                │  /actuator/*  → backend          │
                │  /            → frontend         │
                └──────────────────────────────────┘
                       │                       │
                       ▼                       ▼
            ┌──────────────────┐     ┌────────────────────┐
            │ chores-frontend  │     │  chores-backend    │
            │ Next.js 15 SSR   │     │  Spring Boot 3     │
            │ port 3000        │     │  port 8080         │
            └──────────────────┘     └─────────┬──────────┘
                                                │ JDBC, creds from shared-postgres-secret
                                                ▼
                                     ┌────────────────────────────────┐
                                     │   shared-postgres (cluster)    │
                                     │   db: kidstasks  user: kidstasks│
                                     │   shared with other custom apps│
                                     └────────────────────────────────┘
```

## Why path-routed Ingress (not subdomain split)

Single hostname is simpler:
- one cert (Tailscale auto-mints LetsEncrypt-style certs per ingress)
- the browser treats `/api/*` as same-origin, so no CORS pre-flights and the JWT is sent with every fetch using a relative URL (`/api/...`)
- you don't need a separate `api.chores.stoat-perch.ts.net` ingress

## Auth

- Login: `POST /api/auth/login` returns a JWT (HS256, 30-day TTL) + the user profile.
- The frontend stores the JWT in `localStorage` (`chores.token`) and sends `Authorization: Bearer …` on every API call.
- The backend has a `JwtAuthFilter` that parses the token, builds an `AuthUser` principal, and adds the appropriate `ROLE_ADMIN` / `ROLE_KID` authority. Route guards use Spring's `@PreAuthorize`.
- Passwords are stored as bcrypt hashes (`BCryptPasswordEncoder`).
- The JWT secret is a 32-byte base64 value held in `Secret/chores-backend-secret`.

## Data model

```sql
app_user(id, username UQ, password_hash, display_name, role ADMIN|KID, avatar_color,
         edit_window_days INT NOT NULL DEFAULT 14, created_at)
task(id, title, description, points, icon, recurrence DAILY|WEEKLY|ONCE, active, created_at)
task_assignment(id, task_id → task, user_id → app_user, UQ(task_id,user_id))
task_completion(id, task_id → task, user_id → app_user, completion_date, done, completed_at,
                UQ(task_id,user_id,completion_date))
```

Each (task, kid, day) has at most one completion row. The big checkbox toggles `done` between true/false on that row (or creates the row the first time).

`edit_window_days` on `app_user` controls how many past days a kid may tick/untick chores. The service layer in `TaskService.setCompletion` enforces this: requests for dates older than `today - edit_window_days` receive HTTP 400. Admin/parent endpoints are unrestricted. V2 migration adds the column with default 14.

Stats are computed by `StatsService` directly from `task_completion` aggregated by day — no denormalised counters, which keeps writes simple and the kid-count is tiny.

## Frontend structure

```
src/
├─ app/
│  ├─ layout.tsx                 # root (just <body>)
│  ├─ globals.css                # tailwind + kid-check styling
│  ├─ login/page.tsx             # username/password
│  ├─ page.tsx                   # kid: today's tasks (✓/✗ checkbox)
│  ├─ stats/page.tsx             # kid: my stats (Recharts bars)
│  └─ admin/
│     ├─ page.tsx                # admin dashboard (per-kid cards + line chart)
│     ├─ users/page.tsx          # CRUD kids
│     ├─ tasks/page.tsx          # CRUD tasks, assign/unassign per kid
│     └─ settings/page.tsx       # per-kid edit-window configuration
├─ components/
│  ├─ AppShell.tsx               # nav + role guard
│  └─ StatsChart.tsx             # DailyBars + ComparisonLines
└─ lib/
   └─ api.ts                     # typed fetch client (JWT)
```

The route guard is client-side via `AppShell` calling `GET /api/auth/me`. With JWTs that's fine for a household app; for stricter posture we'd move to short-lived access tokens + httpOnly cookies + middleware.

## Backend structure

```
com.nila.chores
├─ ChoresApplication
├─ config/         AdminBootstrap, WebConfig (CORS)
├─ security/       JwtService, JwtAuthFilter, SecurityConfig, AuthController, AuthUser
├─ user/           User entity + repo + service + admin UserController
├─ task/           Task / TaskAssignment / TaskCompletion entities + repos,
│                  TaskService, AdminTaskController, KidTaskController
└─ stats/          StatsService, StatsController
```

Migrations: `src/main/resources/db/migration/V1__init.sql` (Flyway runs at startup).

## Deployment shape

| Resource | Replicas | Storage |
|---|---|---|
| `chores-backend` (Deployment)   | 1 | stateless |
| `chores-frontend` (Deployment)  | 1 | stateless |
| `shared-postgres` (StatefulSet, external to this app) | 1 | shared cluster PVC; chores schema lives in db `kidstasks` |

`imagePullPolicy: Never` — images are built locally on the Mac with `docker build --platform linux/arm64 …` and OrbStack k3s reuses the Mac's Docker daemon. No registry needed for a homelab.

This app contributes two pods (backend + frontend). It does not bring its own DB — see [Shared Postgres](../shared-postgres/README.md). That saves a Postgres pod + 5Gi PVC and centralises backups under one `pg_dumpall`.

## Why these choices

- **Spring Boot 3 + JPA** — productive for a small CRUD app, easy auth + validation, and the user wanted a "super rich Java backend." Could've been a single-file Express server; Spring lets us add tests, transactions, validation, and migrations without ceremony.
- **Next.js 15 (App Router) + Tailwind + Recharts** — the current "default modern UX framework" combo. App Router gives us nested layouts; Tailwind makes responsive mobile-first trivial; Recharts is the cleanest declarative charts lib for React.
- **JWT in localStorage** — simplest workable model for a same-origin app. Acceptable threat model for a household tracker.
- **Shared Postgres (not dedicated)** — the cluster already provisioned a `shared-postgres` instance with a pre-baked `kidstasks` DB + user. Reusing it saves a Postgres pod + 5Gi PVC and means one backup script covers every custom app. Schema isolation is per-DB: chores can't see other apps' data.
- **Path-routed Tailscale Ingress** — one hostname, no CORS, no separate proxy.
