#!/usr/bin/env bash
# undeploy.sh — tear down chores deployments/services/ingress/secrets
# Preserves data: PVCs/PVs are NOT deleted (chores has none, but the pattern
# is consistent across all homelab apps).
# The shared-postgres-secret is NOT deleted — other apps may depend on it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Load .env for NAMESPACE ───────────────────────────────────────────────────
ENV_FILE="$SCRIPT_DIR/.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
fi
NAMESPACE="${NAMESPACE:-homelab}"

echo "Undeploying chores from namespace '$NAMESPACE'..."
echo "(PVCs/PVs are NOT deleted. shared-postgres-secret is NOT deleted.)"
echo ""

# ── Deployments ───────────────────────────────────────────────────────────────
kubectl -n "$NAMESPACE" delete deployment chores-backend  --ignore-not-found
kubectl -n "$NAMESPACE" delete deployment chores-frontend --ignore-not-found

# ── Services ──────────────────────────────────────────────────────────────────
kubectl -n "$NAMESPACE" delete service chores-backend  --ignore-not-found
kubectl -n "$NAMESPACE" delete service chores-frontend --ignore-not-found

# ── Ingress ───────────────────────────────────────────────────────────────────
kubectl -n "$NAMESPACE" delete ingress chores --ignore-not-found

# ── App secret (chores-only — NOT shared-postgres-secret) ────────────────────
kubectl -n "$NAMESPACE" delete secret chores-backend-secret --ignore-not-found

echo ""
echo "✓ Chores torn down."
echo ""
echo "  Kept (data):"
echo "    - shared-postgres-secret (shared with other apps)"
echo "    - Any PVCs/PVs (chores has none, but noted for consistency)"
echo ""
echo "  To fully wipe chores data from Postgres:"
echo "    kubectl -n $NAMESPACE exec shared-postgres-0 -- \\"
echo "      psql -U kidstasks -d kidstasks -c 'DROP SCHEMA public CASCADE; CREATE SCHEMA public;'"
echo ""
echo "  To redeploy:  ./deploy.sh"
