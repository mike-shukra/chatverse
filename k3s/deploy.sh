#!/bin/bash
set -e

PROJECT_ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAMESPACE="chatverse"
APP_NAME="chatverse-app"
IMAGE_NAME="yourusername/chatverse-app:arm64"

apply_manifest() {
  echo "🔹 Applying $1..."
  kubectl apply -f "$1"
}

apply_ns_manifest() {
  echo "🔹 Applying $1 in namespace $NAMESPACE..."
  kubectl apply -n "$NAMESPACE" -f "$1"
}

# Создаем namespace
apply_manifest "${PROJECT_ROOT_DIR}/k8s/namespace.yaml"

# Установка зависимостей
apply_ns_manifest "${PROJECT_ROOT_DIR}/k8s/postgres.yaml"
apply_ns_manifest "${PROJECT_ROOT_DIR}/k8s/redis.yaml"
apply_ns_manifest "${PROJECT_ROOT_DIR}/k8s/kafka.yaml"

# Деплой приложения
echo "🚀 Deploying application..."
envsubst < "${PROJECT_ROOT_DIR}/k8s/app.template.yaml" | kubectl apply -n "$NAMESPACE" -f -

# Применяем Ingress
apply_ns_manifest "${PROJECT_ROOT_DIR}/k8s/ingress.yaml"

echo "✅ Deployment completed!"