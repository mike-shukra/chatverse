#!/bin/bash
# Этот скрипт проверяет доступность приложения через NodePort Ingress,
# используя IP адрес ноды кластера Kind (т.к. localhost:NodePort часто не работает в WSL).

# Предполагается, что переменные окружения (NAMESPACE, APP_NAME, KIND_CLUSTER_NAME,
# HELM_INGRESS_NAMESPACE, HELM_INGRESS_RELEASE_NAME, INGRESS_HOST)
# и функции (info, warn, error_exit) доступны из родительского скрипта при вызове через source.

# Если скрипт запускается напрямую, нужно определить переменные и функции.
# Для простоты предполагаем запуск через source из deploy*.sh

echo ""
echo "=============================="
echo "🧪 Проверка доступа к приложению (через NodePort Ingress по IP ноды):"
echo "=============================="

# 1. Получаем NodePort сервиса ingress-контроллера
info "Получение NodePort для сервиса '${HELM_INGRESS_RELEASE_NAME}-controller'..."
INGRESS_NODE_PORT=$(kubectl get service -n "$HELM_INGRESS_NAMESPACE" "${HELM_INGRESS_RELEASE_NAME}-controller" -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}' 2>/dev/null)
kubectl_get_svc_exit_code=$?

if [[ $kubectl_get_svc_exit_code -ne 0 ]] || [[ -z "$INGRESS_NODE_PORT" ]]; then
    error_exit "Не удалось получить NodePort для ingress-nginx-controller ('${HELM_INGRESS_RELEASE_NAME}-controller' в ns '$HELM_INGRESS_NAMESPACE'). Код kubectl: $kubectl_get_svc_exit_code"
fi
info "Используется NodePort: $INGRESS_NODE_PORT"

# 2. Получаем IP адрес ноды Kind
info "Получение IP адреса ноды кластера Kind '${KIND_CLUSTER_NAME}-control-plane'..."
NODE_IP=$(kubectl get node "${KIND_CLUSTER_NAME}-control-plane" -o jsonpath='{.status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null)
kubectl_get_node_exit_code=$?

if [[ $kubectl_get_node_exit_code -ne 0 ]] || [[ -z "$NODE_IP" ]]; then
     error_exit "Не удалось получить IP ноды Kind ('${KIND_CLUSTER_NAME}-control-plane'). Код kubectl: $kubectl_get_node_exit_code."
fi
info "Используется IP ноды: $NODE_IP"

# 3. Формируем URL для проверки
APP_URL="http://${NODE_IP}:${INGRESS_NODE_PORT}"
# INGRESS_HOST должен быть определен в родительском скрипте (например, "chatverse.local")
if [[ -z "${INGRESS_HOST:-}" ]]; then
    error_exit "Переменная INGRESS_HOST не определена в родительском скрипте."
fi

# 4. Выполняем проверку через curl и jq
info "📡 curl: Actuator Health (форматированный JSON) ($APP_URL/actuator/health, Host: $INGRESS_HOST)"
health_output=$(curl -L --fail -s --connect-timeout 10 -H "Host: $INGRESS_HOST" "$APP_URL/actuator/health")
curl_exit_code=$?

# Проверяем результат curl и валидность JSON
if [[ $curl_exit_code -eq 0 ]] && echo "$health_output" | jq '.' > /dev/null 2>&1; then
    # Успех: выводим отформатированный JSON
    echo "$health_output" | jq '.'
    echo ""
    echo "✅ Actuator Health доступен через IP ноды (${NODE_IP}). Приложение '$APP_NAME' работает!"
    # Не выходим с exit 0, чтобы родительский скрипт мог продолжить
else
    # Ошибка
    error_exit "Actuator endpoint недоступен через IP ноды (${NODE_IP}) (curl exit code: $curl_exit_code), либо вывод не является валидным JSON. Проверь логи приложения '$APP_NAME' или ingress-контроллера."
fi

# Конец скрипта check-health.sh