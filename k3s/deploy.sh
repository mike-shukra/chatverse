#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K3S_DIR="$PROJECT_DIR/k3s"
NAMESPACE="chatverse"

# Определяем имя пользователя для Docker-образа
DOCKER_USERNAME=${DOCKER_USERNAME:-$(whoami)}
IMAGE_NAME="${DOCKER_USERNAME}/chatverse-app:arm64"

# Fix kubeconfig permissions
[ ! -r "/etc/rancher/k3s/k3s.yaml" ] && sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# Fix permissions
cd "$PROJECT_DIR"
[ -f "gradlew" ] && chmod +x gradlew

# Build
echo "🔨 Building application..."
./gradlew clean build -x test

# Docker build
echo "🐳 Building Docker image as ${IMAGE_NAME}..."
docker build -t "$IMAGE_NAME" .

# Deploy
echo "🚀 Deploying to k3s..."
kubectl apply -f "$K3S_DIR/namespace.yaml"
kubectl apply -f "$K3S_DIR/postgres.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/redis.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/kafka.yaml" -n $NAMESPACE

# Deploy app with substituted image name
export IMAGE_NAME
envsubst < "$K3S_DIR/app.template.yaml" | kubectl apply -n $NAMESPACE -f -

# Apply ingress
kubectl apply -f "$K3S_DIR/ingress.yaml" -n $NAMESPACE

echo "✅ Deployment complete! Image: ${IMAGE_NAME}"
kubectl get pods -n $NAMESPACE