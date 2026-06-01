# Chores — kids' chore tracker

Custom app built for this homelab. Parent (admin) creates kid accounts + chores, assigns each chore to one or more kids. Each kid signs in on their own phone, sees today's chore list as big tappable checkboxes, and ticks them ✓ / ✗. Admin sees a per-kid completion-rate dashboard with a daily comparison chart.

Source: `/Users/nila/Developer/apps/chores/`

**On this page:** [Access](#access) · [Initial credentials](#initial-credentials) · [What it does](#what-it-does) · [Stack & framework](#stack--framework) · [Storage](#storage) · [See also](#see-also) · [File reference](#file-reference)

---

## Access

| Where | URL |
|---|---|
| **iPhone / kids / family on Tailscale** | https://chores.stoat-perch.ts.net |
| **This Mac (browser, localhost)** | http://localhost:3000 *(only when the port-forward is running — see [LAN + localhost access](#lan--localhost-access-optional))* |
| **LAN devices** (TV, other laptops) | http://192.168.68.57:3000 *(same condition as localhost)* |
| **Cluster DNS — frontend** (other pods / Mac shell) | http://chores-frontend.homelab.svc.cluster.local |
| **Cluster DNS — backend API** (curl / Postman) | http://chores-backend.homelab.svc.cluster.local:8080 |
| **Ad-hoc debug port-forward** | `kubectl -n homelab port-forward svc/chores-frontend 3000:3000` |

The HTTPS Tailscale URL is the only one your kids ever need. Everything else is for you when debugging.

### LAN + localhost access (optional)

By default the app is only reachable via Tailscale + cluster DNS. To also expose it on `http://localhost:3000` (from this Mac) and `http://192.168.68.57:3000` (from other LAN devices), wire it into the existing launchd port-forward script:

1. Edit `~/homelab/localhost-portforward.sh` and add a line:
   ```bash
   kubectl port-forward svc/chores-frontend 3000:3000 -n homelab --address 0.0.0.0 &
   CHORES_PID=$!
   ```
   Update the `trap` line at the bottom of the script to include `$CHORES_PID` so it's killed cleanly on stop.
2. Reload:
   ```bash
   ~/homelab/refresh-localhost.sh
   ```

(Pattern matches what immich/jellyfin/docs already do — see [Add a new app §5](../../ADD-NEW-APP.md).)

---

## Initial credentials

| | |
|---|---|
| User | `admin` |
| Password | `admin` |

**Change immediately** — see [Maintenance → Rotate admin password](MAINTENANCE.md#rotate-admin-password). The bootstrap admin is only created on first start when no admin exists, so editing the Secret afterwards does nothing — you must update the row in Postgres.

---

## What it does

- ✅ Admin creates kid accounts with their own username + password
- ✅ Admin defines chores (title, optional description, points, emoji icon, recurrence: daily / weekly / once)
- ✅ Admin assigns each chore to one or more specific kids
- ✅ Kid signs in, sees their own list — taps the big square checkbox to flip between ✗ (not done) and ✓ (done) for today
- ✅ Each kid only sees their own chores and their own stats — never another kid's data
- ✅ Mobile-friendly responsive UI — works in iPhone/Android Safari/Chrome, no app needed
- ✅ Admin dashboard: per-kid card with completion-rate %, plus a multi-line daily-comparison chart
- ✅ Each kid has a personal stats page with a bar chart showing chores done per day (7/14/30-day window)

---

## Stack & framework

| Layer | Tech |
|---|---|
| Backend | Java 21 + **Spring Boot 3.4** (web, data-jpa, security, validation, actuator) |
| Migrations | **Flyway** (`V1__init.sql` runs on startup) |
| Auth | **JWT** (HS256, 30-day TTL) + bcrypt password hashes |
| Database | **Shared cluster Postgres** — `shared-postgres.homelab.svc.cluster.local`, database `kidstasks`, schema owned by user `kidstasks`. See [Shared Postgres](../shared-postgres/README.md). |
| Frontend | **Next.js 15** (App Router, standalone build) + React 19 + **TypeScript** |
| Styling | **Tailwind CSS** |
| Charts | **Recharts** (BarChart for kid stats, LineChart for admin comparison) |
| Deploy | Kubernetes (`homelab` namespace), Tailscale Ingress, single hostname with path-routing |

---

## Storage

This app does not own its database — it uses the cluster's [Shared Postgres](../shared-postgres/README.md) at `shared-postgres.homelab.svc.cluster.local:5432`, database `kidstasks`. The schema is created on startup by Flyway (`V1__init.sql`). Credentials are read from Secret `chores-postgres-secret` (keys `KIDSTASKS_DB`, `KIDSTASKS_USER`, `KIDSTASKS_PASSWORD`).

Backend and frontend pods are stateless — no PVCs in this app's manifests.

---

## See also

- [Usage guide](USER-MANUAL.md) — daily workflows for admin and kid
- [Maintenance](MAINTENANCE.md) — where Postgres lives, how to access it, rebuilds, troubleshooting
- [Architecture](ARCHITECTURE.md) — request flow, data model, auth model

## File reference

| File | Purpose |
|---|---|
| `/Users/nila/Developer/apps/chores/backend/` | Spring Boot source |
| `/Users/nila/Developer/apps/chores/frontend/` | Next.js source |
| `/Users/nila/Developer/apps/chores/k8s/10-backend.yaml` | Backend Deployment + Service + Secret (wires DB env from `chores-postgres-secret`) |
| `/Users/nila/Developer/apps/chores/k8s/20-frontend.yaml` | Frontend Deployment + Service |
| `/Users/nila/Developer/apps/chores/k8s/30-ingress.yaml` | Tailscale Ingress (path-routed) |
| `~/homelab/shared-postgres.yaml` | Shared Postgres StatefulSet (not owned by this app — see [Shared Postgres](../shared-postgres/README.md)) |
