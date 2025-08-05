#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K3S_DIR="$PROJECT_DIR/k3s"
NAMESPACE="chatverse"

# 1. Сборка приложения (на Raspberry Pi)
echo "🔨 Building application with Gradle..."
cd "$PROJECT_DIR"
./gradlew clean build -x test

# 2. Сборка Docker-образа
echo "🐳 Building Docker image..."
docker build -t yourusername/chatverse-app:arm64 .

# 3. Применение конфигураций Kubernetes
echo "🚀 Deploying to k3s..."
kubectl apply -f "$K3S_DIR/namespace.yaml"
kubectl apply -f "$K3S_DIR/postgres.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/redis.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/kafka.yaml" -n $NAMESPACE

# 4. Развертывание приложения
export IMAGE_NAME="yourusername/chatverse-app:arm64"
envsubst < "$K3S_DIR/app.template.yaml" | kubectl apply -n $NAMESPACE -f -

# 5. Применение Ingress
kubectl apply -f "$K3S_DIR/ingress.yaml" -n $NAMESPACE

echo "✅ Deployment complete!"
kubectl get pods -n $NAMESPACE