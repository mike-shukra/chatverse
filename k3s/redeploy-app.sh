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

echo "🧹 Удаляем предыдущую версию приложения..."
kubectl delete -f "$K3S_DIR/app.template.yaml" --ignore-not-found
kubectl delete -f "$K3S_DIR/ingress.yaml" --ignore-not-found

# Удаляем старые образы приложения
echo "🗑️ Удаляем старые Docker-образы приложения..."
docker rmi -f $(docker images | grep "chatverse-app" | awk '{print $3}') || true

# Переходим в директорию проекта
cd "$PROJECT_DIR"

# Сборка приложения
echo "🔨 Собираем приложение..."
./gradlew clean build -x test

# Сборка Docker-образа
echo "🐳 Собираем Docker-образ ${IMAGE_NAME}..."
docker build --platform linux/arm64 -t "$IMAGE_NAME" .

# Проверка, что образ создался
if ! docker inspect "$IMAGE_NAME" &> /dev/null; then
  echo "❌ Docker-образ не найден!"
  exit 1
fi

# Деплой приложения
echo "🚀 Развертываем приложение..."
export IMAGE_NAME
envsubst < "$K3S_DIR/app.template.yaml" | kubectl apply -n $NAMESPACE -f -

# Применяем Ingress
kubectl apply -f "$K3S_DIR/ingress.yaml" -n $NAMESPACE

echo "✅ Приложение успешно развернуто! Образ: ${IMAGE_NAME}"

# Мониторинг статуса
echo "🔍 Отслеживаем статус пода..."
kubectl get pods -n $NAMESPACE -l app=chatverse-app -w