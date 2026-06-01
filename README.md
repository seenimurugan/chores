# chores

Kids' chore tracker — Spring Boot 3 backend + Next.js 15 frontend on Kubernetes.

## What it does

Parent (admin) creates kid accounts and assigns chores with recurrence and points. Each kid signs in on their phone and taps big checkboxes to mark chores done. Admin sees a per-kid completion-rate dashboard with a daily comparison line chart.

## Depends on

- **cluster-setup** — `homelab` namespace, Tailscale ingress controller: [`github.com/seenimurugan/homelab-cluster-setup`](https://github.com/seenimurugan/homelab-cluster-setup)
- **shared-postgres** — `shared-postgres.homelab.svc.cluster.local`, database `kidstasks`, creds in `chores-postgres-secret`: [`github.com/seenimurugan/homelab-shared-postgres`](https://github.com/seenimurugan/homelab-shared-postgres)

## Quick start

```bash
git clone https://github.com/seenimurugan/chores
cd chores

# 1. Set up your env
cp .env.example .env
$EDITOR .env   # fill in CHORES_JWT_SECRET, KIDSTASKS_PASSWORD, etc.

# 2. Build images (arm64, local — OrbStack k3s shares the Mac's Docker daemon)
docker build --platform linux/arm64 -t chores-backend:0.3  backend/
docker build --platform linux/arm64 -t chores-frontend:0.4 frontend/

# 3. Deploy
./deploy.sh
```

`deploy.sh` is idempotent — safe to re-run. It creates/updates secrets, applies manifests via `envsubst`, and waits for rollouts.

## Access

| | |
|---|---|
| **Tailnet URL** | https://chores.stoat-perch.ts.net |
| **Admin login** | `admin` / see `.env` (`CHORES_ADMIN_PASSWORD`) |
| **Debug port-forward** | `kubectl -n homelab port-forward svc/chores-frontend 3000:3000` |

Rotate the admin password on first deploy — the default `changeme` is only used to bootstrap and is not changed by updating the Secret. See [docs/MAINTENANCE.md](docs/MAINTENANCE.md#rotate-admin-password).

## Tear down

```bash
./undeploy.sh   # removes deployments/services/ingress/secrets; preserves Postgres data
```

## Docs

- [docs/README.md](docs/README.md) — app overview, access URLs, stack summary
- [docs/USER-MANUAL.md](docs/USER-MANUAL.md) — admin and kid usage guide
- [docs/MAINTENANCE.md](docs/MAINTENANCE.md) — Postgres access, password rotation, rebuilds, troubleshooting
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — request flow, data model, auth design

Also rendered live at https://docs.stoat-perch.ts.net (sidebar → Chores).
