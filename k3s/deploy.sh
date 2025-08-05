#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K3S_DIR="$PROJECT_DIR/k3s"
NAMESPACE="chatverse"

# Fix kubeconfig permissions
if [ ! -r "/etc/rancher/k3s/k3s.yaml" ]; then
    echo "🔧 Fixing kubeconfig permissions..."
    sudo chmod 644 /etc/rancher/k3s/k3s.yaml
fi
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# Fix Gradle Wrapper permissions
echo "🔧 Fixing permissions..."
cd "$PROJECT_DIR"
[ -f "gradlew" ] && chmod +x gradlew

# Build application
echo "🔨 Building application with Gradle..."
./gradlew clean build -x test

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t yourusername/chatverse-app:arm64 .

# Deploy to k3s
echo "🚀 Deploying to k3s..."
kubectl apply -f "$K3S_DIR/namespace.yaml"
kubectl apply -f "$K3S_DIR/postgres.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/redis.yaml" -n $NAMESPACE
kubectl apply -f "$K3S_DIR/kafka.yaml" -n $NAMESPACE

# Deploy application
export IMAGE_NAME="yourusername/chatverse-app:arm64"
envsubst < "$K3S_DIR/app.template.yaml" | kubectl apply -n $NAMESPACE -f -

# Apply Ingress
kubectl apply -f "$K3S_DIR/ingress.yaml" -n $NAMESPACE

echo "✅ Deployment complete!"
kubectl get pods -n $NAMESPACE