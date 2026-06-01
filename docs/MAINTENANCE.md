# Chores — maintenance

Operational runbook. Everything lives in the `homelab` namespace.

**On this page:** [Quick reference](#quick-reference) · [Cluster topology](#cluster-topology) · [Framework & versions](#framework--versions) · [Quick health checks](#quick-health-checks) · [Postgres access](#postgres-access) · [Rebuilding after a code change](#rebuilding-after-a-code-change) · [Rotate admin password](#rotate-admin-password) · [Rotate the JWT signing secret](#rotate-the-jwt-signing-secret) · [Backups](#backups) · [Logs](#logs) · [Troubleshooting](#troubleshooting) · [Updates](#updates) · [File reference](#file-reference)

---

## Quick reference

Everything you'll need when something breaks, in one block.

### Source code on this Mac

| What | Path |
|---|---|
| Backend (Spring Boot, Java 21) | `/Users/nila/Developer/apps/chores/backend/` |
| Frontend (Next.js 15) | `/Users/nila/Developer/apps/chores/frontend/` |
| Kubernetes manifests | `/Users/nila/Developer/apps/chores/k8s/` |
| Project README (quick deploy commands) | `/Users/nila/Developer/apps/chores/README.md` |
| These docs | `/Users/nila/Developer/agents/docs/homelab-k8s-setup/apps/chores/` |
| Backend version source-of-truth | `/Users/nila/Developer/apps/chores/backend/pom.xml` |
| Frontend version source-of-truth | `/Users/nila/Developer/apps/chores/frontend/package.json` |

### Live in the cluster

| What | Where |
|---|---|
| Public URL | https://chores.stoat-perch.ts.net |
| Backend pod | `Deployment/chores-backend` (port 8080) in `homelab` ns |
| Frontend pod | `Deployment/chores-frontend` (port 3000) in `homelab` ns |
| Ingress | `Ingress/chores` (Tailscale, path-routed) |
| Backend image | `chores-backend:0.1` (local, `imagePullPolicy: Never`) |
| Frontend image | `chores-frontend:0.1` (local, `imagePullPolicy: Never`) |

### Database (uses cluster's shared Postgres — chores does NOT run its own)

| What | Value |
|---|---|
| Host (DNS) | `shared-postgres.homelab.svc.cluster.local` |
| Port | `5432` |
| Database | `kidstasks` |
| User | `kidstasks` |
| Password (read from Secret) | `kubectl -n homelab get secret chores-postgres-secret -o jsonpath='{.data.KIDSTASKS_PASSWORD}' \| base64 -d; echo` |
| Open a psql shell | `kubectl -n homelab exec -it shared-postgres-0 -- psql -U kidstasks kidstasks` |
| List chores tables | `kubectl -n homelab exec shared-postgres-0 -- psql -U kidstasks -d kidstasks -c '\dt'` |
| GUI access (TablePlus / DBeaver) | `kubectl -n homelab port-forward svc/shared-postgres 5432:5432`, then localhost:5432 |
| Full Postgres ops (backup, restart…) | [Shared Postgres docs](../shared-postgres/README.md) |

### Other secrets

| Secret | Keys | Purpose |
|---|---|---|
| `chores-backend-secret` | `CHORES_JWT_SECRET`, `CHORES_ADMIN_USERNAME`, `CHORES_ADMIN_PASSWORD`, `CHORES_ADMIN_DISPLAY_NAME` | JWT signing key + bootstrap admin |
| `chores-postgres-secret` | `KIDSTASKS_DB`, `KIDSTASKS_USER`, `KIDSTASKS_PASSWORD` | DB creds for chores |

### One-liners worth memorising

```bash
# Are my pods up?
kubectl -n homelab get pods -l 'app in (chores-backend,chores-frontend)'

# Tail backend logs
kubectl -n homelab logs deploy/chores-backend --tail=200 -f

# Restart backend (after a rebuild)
kubectl -n homelab rollout restart deploy/chores-backend

# Tail frontend logs
kubectl -n homelab logs deploy/chores-frontend --tail=200 -f

# Get me a DB shell
kubectl -n homelab exec -it shared-postgres-0 -- psql -U kidstasks kidstasks
```

If you've never deployed this app before: see the project [README quick deploy](../../README.md) (or `/Users/nila/Developer/apps/chores/README.md` on this Mac).

---

## Cluster topology

```
                Tailscale (chores.stoat-perch.ts.net)
                              │
                              ▼
                ┌────────── Ingress/chores ───────────┐
                │                                      │
                │  /api/*       → chores-backend       │
                │  /actuator/*  → chores-backend       │
                │  /            → chores-frontend      │
                └──────────────────────────────────────┘
                       │                       │
                       ▼                       ▼
            chores-frontend (Next.js)   chores-backend (Spring Boot 3)
                  port 3000                  port 8080
                                                  │ JDBC (DB_HOST/DB_NAME/USER/PASSWORD env)
                                                  ▼
                            shared-postgres.homelab.svc.cluster.local:5432
                            database: kidstasks   user: kidstasks
                            (owned by the cluster, NOT by this app)
```

This app does **not** run its own Postgres. It connects to the cluster's shared Postgres — see [Shared Postgres](../shared-postgres/README.md) for the StatefulSet, backup story, and credential layout. Adding the chores app didn't require a new DB pod; the `kidstasks` DB + user already existed there.

| Resource | Type | Image / notes |
|---|---|---|
| `chores-backend` | Deployment (1) | `chores-backend:0.1` (locally built, arm64, `imagePullPolicy: Never`) |
| `chores-frontend` | Deployment (1) | `chores-frontend:0.1` (locally built, arm64, `imagePullPolicy: Never`) |
| `chores` | Ingress | Tailscale (path-routed, single hostname) |
| `chores-backend-secret` | Secret | `CHORES_JWT_SECRET`, `CHORES_ADMIN_USERNAME`, `CHORES_ADMIN_PASSWORD`, `CHORES_ADMIN_DISPLAY_NAME` |
| `chores-postgres-secret` | Secret | DB creds — chores reads `KIDSTASKS_DB`, `KIDSTASKS_USER`, `KIDSTASKS_PASSWORD` |
| `shared-postgres-0` | StatefulSet pod (existing, shared) | Owned by the cluster, not by this app |

---

## Framework & versions

| Layer | Choice | Why |
|---|---|---|
| Backend language | Java 21 (Temurin) | LTS, virtual threads available, fast startup |
| Backend framework | **Spring Boot 3.4** | Productive CRUD, JPA + Security + Validation + Actuator in one |
| ORM / migrations | Spring Data JPA + **Flyway** | `V1__init.sql` runs on first start; `ddl-auto: validate` so schema can't drift |
| Auth | JJWT 0.12 (HS256) + BCryptPasswordEncoder | JWT in `Authorization: Bearer`, 30-day TTL |
| Build | Maven (run inside Docker — no local mvn install needed) | One-shot reproducible build |
| Database | **PostgreSQL 17** via cluster's `shared-postgres` | Reuses an existing cluster service — saves a pod + 5Gi PVC and centralises backups |
| Frontend framework | **Next.js 15** (App Router, standalone output) | SSR + RSC where useful, single-binary container |
| Frontend lang | TypeScript 5.7 + React 19 | Strict types, latest concurrent React |
| Styling | **Tailwind CSS 3.4** | Mobile-first responsive without writing CSS |
| Charts | **Recharts 2.15** | Declarative React charts; BarChart + LineChart |
| Container runtime | Docker (OrbStack) → k3s | OrbStack k3s reuses Mac docker daemon, so `imagePullPolicy: Never` works |

Source of truth for versions:
- `/Users/nila/Developer/apps/chores/backend/pom.xml`
- `/Users/nila/Developer/apps/chores/frontend/package.json`

---

## Quick health checks

```bash
# Are all 3 pods up?
kubectl -n homelab get pods -l 'app in (chores-backend,chores-frontend)'
# (DB pod lives under shared-postgres — `kubectl -n homelab get pod shared-postgres-0`)

# Backend health (from inside the cluster)
kubectl -n homelab exec deploy/chores-backend -- wget -qO- http://localhost:8080/actuator/health

# Does the Ingress have a tailnet hostname?
kubectl -n homelab get ingress chores
# Expect: ADDRESS = chores.stoat-perch.ts.net
```

---

## Postgres access

The chores app uses the cluster's **shared Postgres** — not a dedicated one. Full operator notes for the Postgres pod itself are in [Shared Postgres](../shared-postgres/README.md). What's specific to chores:

### Address (cluster-internal)

| | |
|---|---|
| Host (DNS) | `shared-postgres.homelab.svc.cluster.local` |
| Host (short form, from inside `homelab` ns) | `shared-postgres` |
| Port | `5432` |
| Database | `kidstasks` |
| User | `kidstasks` (owns the schema; bcrypt'd via Flyway) |
| Password | Stored in Secret `chores-postgres-secret`, key `KIDSTASKS_PASSWORD` |

The backend reads these via env, wired in `k8s/10-backend.yaml`:
```yaml
- name: DB_HOST   value: shared-postgres.homelab.svc.cluster.local
- name: DB_PORT   value: "5432"
- name: DB_NAME       valueFrom: { secretKeyRef: { name: chores-postgres-secret, key: KIDSTASKS_DB } }
- name: SPRING_DATASOURCE_USERNAME  valueFrom: { secretKeyRef: { name: chores-postgres-secret, key: KIDSTASKS_USER } }
- name: SPRING_DATASOURCE_PASSWORD  valueFrom: { secretKeyRef: { name: chores-postgres-secret, key: KIDSTASKS_PASSWORD } }
- name: SPRING_DATASOURCE_URL       value: jdbc:postgresql://$(DB_HOST):$(DB_PORT)/$(DB_NAME)
```

### Open a `psql` shell

```bash
# Interactive shell as the kidstasks user, on the kidstasks database
kubectl -n homelab exec -it shared-postgres-0 -- psql -U kidstasks kidstasks

# One-off query (e.g. list tables)
kubectl -n homelab exec shared-postgres-0 -- psql -U kidstasks -d kidstasks -c '\dt'
```

### See the current password

```bash
kubectl -n homelab get secret chores-postgres-secret \
  -o jsonpath='{.data.KIDSTASKS_PASSWORD}' | base64 -d; echo
```

### Connect from a GUI (TablePlus / DBeaver / pgAdmin)

```bash
kubectl -n homelab port-forward svc/shared-postgres 5432:5432
```

Connect the GUI to `localhost:5432`, db `kidstasks`, user `kidstasks`, password from the secret above.

### Rotate the chores DB password

```bash
NEW_PWD="strong-password-here"
kubectl -n homelab patch secret chores-postgres-secret --type=json -p="[
  {\"op\":\"replace\",\"path\":\"/data/KIDSTASKS_PASSWORD\",\"value\":\"$(echo -n "$NEW_PWD" | base64)\"}
]"
kubectl -n homelab exec shared-postgres-0 -- psql -U postgres -c \
  "ALTER USER kidstasks WITH PASSWORD '$NEW_PWD';"
kubectl -n homelab rollout restart deploy/chores-backend
```

### Tables

```
app_user           — users (admin + kids)
task               — chore definitions
task_assignment    — which chore goes to which kid
task_completion    — (task, user, day, done) — the per-day check-off
```

---

## Rebuilding after a code change

The cluster uses **locally built images** (no registry). After editing source, rebuild and roll the pod:

```bash
# Backend
cd /Users/nila/Developer/apps/chores/backend
docker build --platform linux/arm64 -t chores-backend:0.1 .
kubectl -n homelab rollout restart deploy/chores-backend
kubectl -n homelab rollout status   deploy/chores-backend

# Frontend
cd /Users/nila/Developer/apps/chores/frontend
docker build --platform linux/arm64 -t chores-frontend:0.1 .
kubectl -n homelab rollout restart deploy/chores-frontend
kubectl -n homelab rollout status   deploy/chores-frontend
```

Bump the tag (`:0.2`, `:0.3`…) when you want immutable history; update the same tag in `k8s/10-backend.yaml` / `k8s/20-frontend.yaml` and `kubectl apply -f`.

---

## Rotate admin password

The `admin` user is **auto-created only on first boot when no admin exists**. Changing `chores-backend-secret` afterwards does nothing — the bootstrap is skipped. To rotate the admin password, update the bcrypt hash directly in Postgres.

```bash
NEW='your-new-password'

# Generate a bcrypt hash on the Mac
# htpasswd ships with Apache utils. Install once: brew install httpd
HASH=$(htpasswd -bnBC 12 "" "$NEW" | tr -d ':\n' | sed 's/$2y/$2a/')

# Update the row (on shared-postgres, in the kidstasks DB)
kubectl -n homelab exec -i shared-postgres-0 -- \
  psql -U kidstasks -d kidstasks -c \
  "UPDATE app_user SET password_hash='$HASH' WHERE username='admin';"
```

(BCrypt `$2y$` is interoperable with Spring's `BCryptPasswordEncoder`, but we normalise to `$2a$` to be explicit.)

To rotate a **kid's** password, just use the UI: Admin → **Kids** → **Reset password**. No DB query needed.

---

## Rotate the JWT signing secret

Invalidates every issued token — all users must log in again.

```bash
NEW_SECRET=$(openssl rand -base64 32)
kubectl -n homelab patch secret chores-backend-secret \
  -p '{"stringData":{"CHORES_JWT_SECRET":"'"$NEW_SECRET"'"}}'
kubectl -n homelab rollout restart deploy/chores-backend
```

Also update the same value in `/Users/nila/Developer/apps/chores/k8s/10-backend.yaml` so a future `kubectl apply` doesn't revert it.

---

## Backups

Two ways:

**Cluster-wide `pg_dumpall`** — backs up every DB on `shared-postgres` (chores included). This is what should land in the weekly backup script — see [Shared Postgres → Backup](../shared-postgres/README.md#backup-manual).

**Chores-only dump** (smaller, faster):
```bash
mkdir -p ~/homelab-backups/chores
ts=$(date +%Y%m%d-%H%M%S)
kubectl -n homelab exec shared-postgres-0 -- \
  pg_dump -U kidstasks -d kidstasks --no-owner --no-privileges \
  > ~/homelab-backups/chores/chores-$ts.sql
ls -lh ~/homelab-backups/chores | tail -5
```

Restore:
```bash
ts=YYYYMMDD-HHMMSS  # pick a file
kubectl -n homelab exec -i shared-postgres-0 -- \
  psql -U kidstasks -d kidstasks < ~/homelab-backups/chores/chores-$ts.sql
```

---

## Logs

```bash
# Backend (Spring Boot startup + request errors)
kubectl -n homelab logs deploy/chores-backend --tail=200 -f

# Frontend (Next.js runtime)
kubectl -n homelab logs deploy/chores-frontend --tail=200 -f

# Postgres (shared with other apps — useful if migrations fail at boot)
kubectl -n homelab logs shared-postgres-0 --tail=200 -f
```

---

## Troubleshooting

### How to verify the backend can actually reach the DB

Run these in order. The first one that fails tells you where the break is.

```bash
# 1. Is the shared-postgres pod up?
kubectl -n homelab get pod shared-postgres-0
# Expect: Running 1/1

# 2. Can shared-postgres accept a connection right now?
kubectl -n homelab exec shared-postgres-0 -- pg_isready -U kidstasks
# Expect: "accepting connections"

# 3. Are the chores creds in the Secret correct?
kubectl -n homelab get secret chores-postgres-secret \
  -o jsonpath='{.data.KIDSTASKS_PASSWORD}' | base64 -d; echo
# (Should match the password the kidstasks user actually has — if you ran ALTER USER
#  without patching the Secret, this is where the drift shows.)

# 4. Try connecting as the kidstasks user with that password.
PW=$(kubectl -n homelab get secret chores-postgres-secret \
  -o jsonpath='{.data.KIDSTASKS_PASSWORD}' | base64 -d)
kubectl -n homelab exec shared-postgres-0 -- env PGPASSWORD="$PW" \
  psql -h shared-postgres -U kidstasks -d kidstasks -c "select count(*) from app_user;"
# Expect: a count (1 if only the bootstrap admin exists)

# 5. Does the backend pod see those env vars correctly?
kubectl -n homelab exec deploy/chores-backend -- printenv \
  | grep -E "DB_HOST|DB_PORT|DB_NAME|SPRING_DATASOURCE_URL|SPRING_DATASOURCE_USERNAME"

# 6. Does the backend's last startup log show "Successfully applied X migration(s)"?
kubectl -n homelab logs deploy/chores-backend | grep -E "Flyway|Migrating|HikariPool|Started ChoresApplication"
```

If step 1 fails → see [Shared Postgres troubleshooting](../shared-postgres/README.md#troubleshooting).
If step 4 fails but step 2 passes → the Secret value and the actual DB password drifted. Re-run the [Rotate the chores DB password](#rotate-the-chores-db-password) recipe.
If step 5 shows the wrong host/db → the backend Deployment manifest didn't get the latest `kubectl apply`. Re-run `kubectl apply -f /Users/nila/Developer/apps/chores/k8s/10-backend.yaml`.

### Browser shows "Sign in" but `/api/auth/login` returns 401

The credentials are wrong. Double-check the admin password — if you changed it, you changed it in the DB; the `changeme` default no longer works. Verify directly:

```bash
kubectl -n homelab exec shared-postgres-0 -- \
  psql -U kidstasks -d kidstasks -c "SELECT username, role FROM app_user;"
```

### Backend pod in `CrashLoopBackOff`

```bash
kubectl -n homelab logs deploy/chores-backend --tail=200
```

Likely causes:
- **Postgres not reachable** — check `shared-postgres-0` is `Running 1/1`. If `shared-postgres` is down, every app that uses it (including chores) goes down. The backend startup probe waits up to 5 minutes; if Postgres is slower than that, the backend dies. Just delete the failing backend pod once Postgres is up: `kubectl -n homelab delete pod -l app=chores-backend`.
- **`KIDSTASKS_PASSWORD` mismatch** — the value in `chores-postgres-secret` doesn't match the actual DB password (e.g. someone ran `ALTER USER` without patching the secret). Either rotate again with the proper recipe above, or `kubectl exec shared-postgres-0 -- psql -U postgres -c "ALTER USER kidstasks WITH PASSWORD '...'"` to match.
- **Flyway checksum mismatch** — only happens if you edited `V1__init.sql` after first deploy. In dev, blast the chores schema and let Flyway re-run:
  ```bash
  kubectl -n homelab exec shared-postgres-0 -- \
    psql -U kidstasks -d kidstasks -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
  kubectl -n homelab rollout restart deploy/chores-backend
  ```
  ⚠ This only drops the `kidstasks` DB's `public` schema — other apps' data on `shared-postgres` is untouched.

### Frontend works but every API call 404s

The Ingress path order is off. Re-apply:
```bash
kubectl apply -f /Users/nila/Developer/apps/chores/k8s/30-ingress.yaml
```

The order in the manifest matters: more-specific paths (`/api`, `/actuator`) come **before** the catch-all `/`.

### Tailscale hostname not minted

```bash
kubectl -n homelab get ingress chores       # ADDRESS column should show chores.stoat-perch.ts.net
kubectl -n tailscale get pods               # operator pod + a `ts-chores-*` proxy pod
kubectl -n tailscale logs deploy/operator   # look for auth/connection errors
```

The proxy pod needs ~30 seconds after first apply to join the tailnet.

### Frontend pod `CrashLoopBackOff` with "EADDRINUSE" or similar

Just delete the pod — Next.js standalone server is fully stateless.
```bash
kubectl -n homelab delete pod -l app=chores-frontend
```

### "Conversion of type 'number' to type 'string'" at build time

TypeScript strict mode hit in `frontend/src/components/StatsChart.tsx`. Make sure the `ComparisonLines` row type includes `day: string` and the index map is typed `Map<string, Row>` not `Map<string, Record<string, number>>`.

### Reset the whole stack (dev only — destroys all chores data)

Drops just the chores tables on `shared-postgres` (other apps untouched), then redeploys.

```bash
kubectl -n homelab exec shared-postgres-0 -- \
  psql -U kidstasks -d kidstasks -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
kubectl -n homelab delete -f /Users/nila/Developer/apps/chores/k8s/
kubectl apply  -f /Users/nila/Developer/apps/chores/k8s/
```

---

## Updates

Bump the image tag in `k8s/10-backend.yaml` or `k8s/20-frontend.yaml`, then:

```bash
kubectl apply -f /Users/nila/Developer/apps/chores/k8s/10-backend.yaml
kubectl -n homelab rollout status deploy/chores-backend
```

If something breaks: `kubectl -n homelab rollout undo deploy/chores-backend`.

---

## File reference

| File | Purpose |
|---|---|
| `/Users/nila/Developer/apps/chores/backend/` | Spring Boot source |
| `/Users/nila/Developer/apps/chores/frontend/` | Next.js source |
| `/Users/nila/Developer/apps/chores/k8s/10-backend.yaml` | Backend (Secret + Deployment + Service) — pulls DB creds from `chores-postgres-secret` |
| `/Users/nila/Developer/apps/chores/k8s/20-frontend.yaml` | Frontend (Deployment + Service) |
| `/Users/nila/Developer/apps/chores/k8s/30-ingress.yaml` | Tailscale Ingress (path-routed) |
| `/Users/nila/Developer/apps/chores/README.md` | Project quick-start (build & deploy commands) |
| `~/homelab/shared-postgres.yaml` | Shared Postgres — see [Shared Postgres](../shared-postgres/README.md) (not owned by chores) |
