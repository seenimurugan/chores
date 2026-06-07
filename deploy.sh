#!/usr/bin/env bash
# deploy.sh — idempotent deploy for chores (kids' chore tracker)
# Usage: ./deploy.sh
# Safe to re-run; existing resources are patched, not replaced.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── 1. Load .env ─────────────────────────────────────────────────────────────
ENV_FILE="$SCRIPT_DIR/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: .env not found."
  echo "       Copy .env.example to .env and fill in real values, then re-run."
  echo "         cp .env.example .env && \$EDITOR .env"
  exit 1
fi
# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

# ── 2. Prereq checks ─────────────────────────────────────────────────────────
if ! command -v kubectl &>/dev/null; then
  echo "ERROR: kubectl not found in PATH."
  exit 1
fi
if ! command -v envsubst &>/dev/null; then
  echo "ERROR: envsubst not found. Install via: brew install gettext"
  exit 1
fi
if ! kubectl cluster-info &>/dev/null; then
  echo "ERROR: Cannot reach the Kubernetes cluster. Is OrbStack running?"
  exit 1
fi

# ── 3. Ensure namespace exists ───────────────────────────────────────────────
NAMESPACE="${NAMESPACE:-homelab}"
if ! kubectl get namespace "$NAMESPACE" &>/dev/null; then
  echo "Namespace '$NAMESPACE' not found — creating it."
  kubectl create namespace "$NAMESPACE"
else
  echo "Namespace '$NAMESPACE' already exists."
fi

# ── 4. Create / update chores-postgres-secret ────────────────────────────────
# Dedicated per-app secret (split from shared-postgres-secret on 2026-06-01).
# Each app owns its own postgres-credentials secret so no app's deploy.sh can
# stomp another app's keys (root cause of the 2026-06 outage).
# JSON Patch (RFC 6902) on existing secrets is the upsert pattern used across
# the homelab.
echo "Ensuring chores-postgres-secret has KIDSTASKS_* keys (JSON Patch RFC 6902, no stomp)..."
SECRET_NAME="chores-postgres-secret"
_KT_DB_B64=$(printf '%s' "${KIDSTASKS_DB}" | base64 | tr -d '\n')
_KT_USER_B64=$(printf '%s' "${KIDSTASKS_USER}" | base64 | tr -d '\n')
_KT_PASS_B64=$(printf '%s' "${KIDSTASKS_PASSWORD}" | base64 | tr -d '\n')

if kubectl -n "$NAMESPACE" get secret "$SECRET_NAME" &>/dev/null; then
  # Secret already exists — patch only our keys
  kubectl patch secret -n "$NAMESPACE" "$SECRET_NAME" --type=json -p="[
    {\"op\":\"add\",\"path\":\"/data/KIDSTASKS_DB\",\"value\":\"${_KT_DB_B64}\"},
    {\"op\":\"add\",\"path\":\"/data/KIDSTASKS_USER\",\"value\":\"${_KT_USER_B64}\"},
    {\"op\":\"add\",\"path\":\"/data/KIDSTASKS_PASSWORD\",\"value\":\"${_KT_PASS_B64}\"}
  ]"
else
  # Secret does not exist yet (fresh cluster) — create it.
  kubectl -n "$NAMESPACE" create secret generic "$SECRET_NAME" \
    --from-literal=KIDSTASKS_DB="${KIDSTASKS_DB}" \
    --from-literal=KIDSTASKS_USER="${KIDSTASKS_USER}" \
    --from-literal=KIDSTASKS_PASSWORD="${KIDSTASKS_PASSWORD}"
fi

# ── 5. Create / update chores-backend-secret ─────────────────────────────────
echo "Ensuring chores-backend-secret..."
kubectl -n "$NAMESPACE" create secret generic chores-backend-secret \
  --from-literal=CHORES_JWT_SECRET="${CHORES_JWT_SECRET}" \
  --from-literal=CHORES_ADMIN_USERNAME="${CHORES_ADMIN_USERNAME}" \
  --from-literal=CHORES_ADMIN_PASSWORD="${CHORES_ADMIN_PASSWORD}" \
  --from-literal=CHORES_ADMIN_DISPLAY_NAME="${CHORES_ADMIN_DISPLAY_NAME}" \
  --dry-run=client -o yaml | kubectl apply -f -

# ── Apply SealedSecrets (GitOps secrets, encrypted-in-git) ────────────────────
# SealedSecrets in k8s/sealed/ are committed encrypted; the in-cluster
# sealed-secrets controller (kube-system) decrypts them into real Secrets with
# identical values. This is ADDITIVE and the SAFE DR path on a rebuilt cluster.
#
# NOTE: the .env → `kubectl create secret` step above is intentionally KEPT as a
# documented FALLBACK (no big-bang cutover). On a cluster where a plain Secret
# of the same name already exists, the controller will NOT overwrite it unless
# it carries the annotation sealedsecrets.bitnami.com/managed=true — so applying
# these is non-disruptive. See cluster-setup/secrets-dr/README.md for cutover.
SEALED_DIR="$SCRIPT_DIR/k8s/sealed"
if [ -d "$SEALED_DIR" ] && kubectl get crd sealedsecrets.bitnami.com >/dev/null 2>&1; then
  echo "[deploy] Applying SealedSecrets from k8s/sealed/ (controller present)..."
  for f in "$SEALED_DIR"/*.yaml; do
    [ -e "$f" ] || continue
    echo "  → $f"
    kubectl apply -f "$f"
  done
else
  echo "[deploy] SealedSecrets controller not found (crd sealedsecrets.bitnami.com missing) — skipping k8s/sealed/; relying on .env-created Secrets above."
fi

# ── 6. Apply manifests via envsubst ──────────────────────────────────────────
K8S_DIR="$SCRIPT_DIR/k8s"
echo "Applying k8s manifests (envsubst → kubectl apply)..."
for f in "$K8S_DIR"/10-backend.yaml "$K8S_DIR"/20-frontend.yaml "$K8S_DIR"/30-ingress.yaml; do
  echo "  → $f"
  envsubst < "$f" | kubectl apply -f -
done

# ── 7. Wait for rollout ───────────────────────────────────────────────────────
echo "Waiting for chores-backend rollout..."
kubectl -n "$NAMESPACE" rollout status deployment/chores-backend --timeout=5m

echo "Waiting for chores-frontend rollout..."
kubectl -n "$NAMESPACE" rollout status deployment/chores-frontend --timeout=5m

# ── 8. Done ───────────────────────────────────────────────────────────────────
echo ""
echo "✓ chores deployed successfully."
echo ""
echo "  Access (on Tailnet):    https://chores.stoat-perch.ts.net"
echo "  Admin login:            ${CHORES_ADMIN_USERNAME} / (see .env for password)"
echo ""
echo "  Rotate admin password immediately if this is a first deploy:"
echo "  See docs/MAINTENANCE.md#rotate-admin-password"
