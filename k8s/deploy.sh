#!/bin/bash

set -e # Exit immediately if a command exits with a non-zero status.
set -u # Treat unset variables as an error.
# set -o pipefail # Causes a pipeline to return the exit status of the last command in the pipe that failed.


PROJECT_ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# --- Configuration ---
NAMESPACE="chatverse"
APP_NAME="chatverse-app"
KIND_CLUSTER_NAME="my-cluster" # Make kind cluster name a variable
# Use a unique tag, e.g., git hash
GIT_HASH=$(git rev-parse --short HEAD || echo "latest") # Ensure fallback works even if not a git repo
IMAGE_TAG="${GIT_HASH}"
IMAGE_NAME="${APP_NAME}:${IMAGE_TAG}"
PROJECT_ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Define paths to manifests
NS_MANIFEST="$PROJECT_ROOT_DIR/k8s/namespace.yaml"
# INGRESS_DIR is no longer needed for controller installation
POSTGRES_MANIFEST="$PROJECT_ROOT_DIR/k8s/postgres.yaml"
REDIS_MANIFEST="$PROJECT_ROOT_DIR/k8s/redis.yaml"
KAFKA_MANIFEST="$PROJECT_ROOT_DIR/k8s/kafka.yaml"
# Assume you have a template file for app deployment
APP_TEMPLATE_MANIFEST="$PROJECT_ROOT_DIR/k8s/app.template.yaml"
APP_INGRESS_MANIFEST="$PROJECT_ROOT_DIR/k8s/ingress.yaml"
HELM_INGRESS_REPO_NAME="ingress-nginx"
HELM_INGRESS_CHART_NAME="ingress-nginx"
HELM_INGRESS_RELEASE_NAME="ingress-nginx" # Name for the Helm release
HELM_INGRESS_NAMESPACE="ingress-nginx"    # Namespace for Helm release
INGRESS_HOST="chatverse.local"

# --- Helper Functions ---

info() {
    echo "🔹 $1"
}

warn() {
    echo "⚠️ WARNING: $1"
}

error_exit() {
    echo "❌ ERROR: $1" >&2
    exit 1
}

kapply() {
    info "Applying $1..."
    kubectl apply -f "$1"
}

kapply_ns() {
    info "Applying $1 in namespace $NAMESPACE..."
    kubectl apply -n "$NAMESPACE" -f "$1"
}

kdelete_ns() {
    info "Deleting $1 in namespace $NAMESPACE (if exists)..."
    kubectl delete -n "$NAMESPACE" "$@" --ignore-not-found=true
}

# Updated kwait function to save logs/describe to files on failure
kwait() {
    local ns_arg=""
    local namespace="" # Store namespace separately for logging
    if [[ -n "$1" ]]; then
      ns_arg="-n $1"
      namespace="$1" # Capture namespace
    fi
    shift # Remove namespace arg if present
    local condition="$1"
    local resource_arg="$2" # e.g., "pod/kafka-0" or "pod -l app=postgres" or "deployment/ingress-nginx-controller"
    local timeout="${3:-120s}"
    local resource_type=""
    local resource_name=""
    local label_selector=""
    local log_dir="$PROJECT_ROOT_DIR/logs" # Define logs directory path

    # Create logs directory if it doesn't exist
    mkdir -p "$log_dir"

    # Determine if it's a label selector or specific resource
    if [[ "$resource_arg" == *"-l "* ]]; then
      label_selector=$(echo "$resource_arg" | sed 's/.*-l //')
      # Sanitize label selector for filename (replace non-alphanumeric with underscore)
      local safe_label_selector=$(echo "$label_selector" | sed 's/[^a-zA-Z0-9]/-/g')
      resource_type="pod" # Assume pod for label selector waits
      info "Waiting for condition '$condition' on $resource_type with label '$label_selector' in namespace '$namespace' (timeout ${timeout})..."

      # Get pod names *before* waiting, in case they disappear quickly after failure
      local initial_pod_names=$(kubectl get pods $ns_arg -l "$label_selector" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true) # Get names, ignore errors for now

      if ! kubectl wait --for="$condition" "$resource_type" -l "$label_selector" $ns_arg --timeout="$timeout"; then
          local timestamp=$(date +%Y%m%d-%H%M%S)
          echo "❌ ERROR: $resource_type with label '$label_selector' did not meet condition '$condition' in time." >&2
          echo "--- Saving logs and describe output for pods matching label '$label_selector' in namespace '$namespace' to '$log_dir' ---" >&2

          # Try to get current pod names again, might differ from initial list
          local current_pod_names=$(kubectl get pods $ns_arg -l "$label_selector" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true)
          # Combine initial and current names, remove duplicates
          local all_pod_names=$(echo "$initial_pod_names $current_pod_names" | tr ' ' '\n' | sort -u | tr '\n' ' ')

          if [[ -n "$all_pod_names" ]]; then
              echo "--- Attempting to get diagnostics for pods (initial/current): $all_pod_names ---" >&2
              for pod_name in $all_pod_names; do
                  # Attempt to save logs/describe for any pod associated with the label, even if gone
                  local log_file="${log_dir}/${pod_name}-${timestamp}.log"
                  local describe_file="${log_dir}/${pod_name}-${timestamp}.describe"
                  echo "--- Saving logs for pod: $pod_name to $log_file ---" >&2
                  # Use --previous flag in case the pod crashed and restarted
                  if ! kubectl logs $ns_arg "$pod_name" --tail=100 > "$log_file" 2>&1; then
                      # If current logs fail, try previous logs
                      if ! kubectl logs $ns_arg "$pod_name" --tail=100 --previous > "$log_file" 2>&1; then
                          echo "    (Could not fetch current or previous logs for $pod_name. Exit code: $?. Pod might be gone.)" >> "$log_file"
                      else
                           echo "    (Saved previous logs for $pod_name)" >> "$log_file" # Indicate previous logs were saved
                      fi
                  fi
                  echo "--- Saving describe for pod: $pod_name to $describe_file ---" >&2
                  if ! kubectl describe pod $ns_arg "$pod_name" > "$describe_file" 2>&1; then
                       echo "    (Could not describe pod $pod_name. Exit code: $?. Pod might be gone.)" >> "$describe_file"
                  fi
              done
          else
              echo "    (No pods found with label '$label_selector' before or after timeout)" >&2
          fi

          # Also save events related to the Deployment/ReplicaSet which manages these pods
          # Find the corresponding ReplicaSet(s)
          local rs_names=$(kubectl get rs $ns_arg -l "$label_selector" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true)
          if [[ -n "$rs_names" ]]; then
              for rs_name in $rs_names; do
                  local rs_events_file="${log_dir}/replicaset-${rs_name}-events-${timestamp}.log"
                  echo "--- Saving events for ReplicaSet '$rs_name' to $rs_events_file ---" >&2
                  kubectl get events $ns_arg --field-selector involvedObject.kind=ReplicaSet,involvedObject.name=$rs_name > "$rs_events_file" 2>&1
              done
          else
               # Fallback: Try finding Deployment if no RS found directly
               local deployment_name=$(kubectl get deployment $ns_arg -l "$label_selector" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
               if [[ -n "$deployment_name" ]]; then
                   local dep_events_file="${log_dir}/deployment-${deployment_name}-events-${timestamp}.log"
                   echo "--- Saving events for Deployment '$deployment_name' to $dep_events_file ---" >&2
                   kubectl get events $ns_arg --field-selector involvedObject.kind=Deployment,involvedObject.name=$deployment_name > "$dep_events_file" 2>&1
               fi
          fi

          # Exit after attempting to save logs
          exit 1
      fi
    else
       # Specific resource name (logic remains similar, simplified as it's one resource)
       resource_type=$(echo "$resource_arg" | cut -d'/' -f1)
       resource_name=$(echo "$resource_arg" | cut -d'/' -f2)
       # Sanitize resource name for filename
       local safe_resource_name=$(echo "$resource_name" | sed 's/[^a-zA-Z0-9]/-/g')
       info "Waiting for condition '$condition' on $resource_arg in namespace '$namespace' (timeout ${timeout})..."
       if ! kubectl wait --for="$condition" "$resource_arg" $ns_arg --timeout="$timeout"; then
           local timestamp=$(date +%Y%m%d-%H%M%S)
           echo "❌ ERROR: $resource_arg did not meet condition '$condition' in time." >&2
           local describe_file="${log_dir}/${resource_type}-${safe_resource_name}-${timestamp}.describe"

           if [[ "$resource_type" == "pod" ]]; then
               local log_file="${log_dir}/${safe_resource_name}-${timestamp}.log"
               echo "--- Saving logs for pod: $resource_name to $log_file ---" >&2
                # Use --previous flag in case the pod crashed and restarted
                if ! kubectl logs $ns_arg "$resource_name" --tail=100 > "$log_file" 2>&1; then
                    # If current logs fail, try previous logs
                    if ! kubectl logs $ns_arg "$resource_name" --tail=100 --previous > "$log_file" 2>&1; then
                        echo "    (Could not fetch current or previous logs for $resource_name. Exit code: $?. Pod might be gone.)" >> "$log_file"
                    else
                        echo "    (Saved previous logs for $resource_name)" >> "$log_file" # Indicate previous logs were saved
                    fi
                fi
               echo "--- Saving describe for pod: $resource_name to $describe_file ---" >&2
               if ! kubectl describe pod $ns_arg "$resource_name" > "$describe_file" 2>&1; then
                    echo "    (Could not describe pod $resource_name. Exit code: $?. Pod might be gone.)" >> "$describe_file"
               fi
           else
               # For non-pod resources, just save describe output
               echo "--- Saving describe for resource: $resource_arg to $describe_file ---" >&2
               if ! kubectl describe "$resource_type" "$resource_name" $ns_arg > "$describe_file" 2>&1; then
                   echo "    (Could not describe $resource_arg. Exit code: $?)" >> "$describe_file"
               fi
           fi
            # Also save events related to the specific resource
            local events_file="${log_dir}/${resource_type}-${safe_resource_name}-events-${timestamp}.log"
            echo "--- Saving events for $resource_type '$resource_name' to $events_file ---" >&2
            kubectl get events $ns_arg --field-selector involvedObject.kind=$resource_type,involvedObject.name=$resource_name > "$events_file" 2>&1

           # Exit after attempting to save logs/describe
           exit 1
       fi
    fi
}

# --- Main Script ---

echo ""
echo "=============================="
echo "🚀 Запуск деплоя Chatverse в Kubernetes..."
echo "=============================="
echo ""

# 🧹 1. Cleanup old resources
info "🧹 Удаление старых ресурсов приложения..."
kdelete_ns deployment $APP_NAME
kdelete_ns svc $APP_NAME
kdelete_ns statefulset kafka
kdelete_ns svc kafka
kdelete_ns deployment postgres
kdelete_ns svc postgres
kdelete_ns deployment redis
kdelete_ns svc redis
kdelete_ns ingress chatverse-ingress

info "🧹 Удаление старого релиза ingress-nginx (Helm)..."
helm uninstall $HELM_INGRESS_RELEASE_NAME --namespace $HELM_INGRESS_NAMESPACE || true # Ignore error if not found

info "🧹 Удаление оставшихся ресурсов ingress-nginx..."
# Keep these as a fallback, especially for cluster-level resources
kubectl delete namespace $HELM_INGRESS_NAMESPACE --ignore-not-found=true
kubectl delete clusterrole $HELM_INGRESS_RELEASE_NAME --ignore-not-found=true # Helm might name roles like release name
kubectl delete clusterrolebinding $HELM_INGRESS_RELEASE_NAME --ignore-not-found=true
kubectl delete clusterrole ingress-nginx --ignore-not-found=true # Just in case old names exist
kubectl delete clusterrolebinding ingress-nginx --ignore-not-found=true
kubectl delete validatingwebhookconfiguration ${HELM_INGRESS_RELEASE_NAME}-admission --ignore-not-found=true # Helm naming convention
kubectl delete mutatingwebhookconfiguration ${HELM_INGRESS_RELEASE_NAME}-admission --ignore-not-found=true # Helm naming convention

sleep 5 # Short sleep after deletions

# 🐳 2. Build and Load Docker Image
info "🐳 Сборка Docker-образа '$IMAGE_NAME'..."
DOCKER_BUILDKIT=1 docker build -t "$IMAGE_NAME" -f "$PROJECT_ROOT_DIR/Dockerfile" "$PROJECT_ROOT_DIR"

info "📤 Загрузка Docker-образа '$IMAGE_NAME' в кластер kind '$KIND_CLUSTER_NAME'..."
kind load docker-image "$IMAGE_NAME" --name "$KIND_CLUSTER_NAME"

# 📦 3. Namespace for App
info "📦 Применение namespace '$NAMESPACE'..."
kapply "$NS_MANIFEST"

# 🏷️ 4. Label Node (Kind specific - still needed for Helm NodePort service)
info "🏷️ Установка метки ingress-ready=true на ноду kind..."
NODE_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.name}') # Safer way for single node
if [[ -n "$NODE_NAME" ]]; then
    kubectl label node "$NODE_NAME" ingress-ready=true --overwrite
else
    warn "Не удалось определить имя ноды для установки метки."
fi

# 🌐 5. Deploy Ingress Controller using Helm
info "🌐 Установка nginx ingress-контроллера с помощью Helm..."
info "Добавление/Обновление репозитория Helm..."
helm repo add $HELM_INGRESS_REPO_NAME https://kubernetes.github.io/ingress-nginx
helm repo update $HELM_INGRESS_REPO_NAME

info "Установка чарта $HELM_INGRESS_CHART_NAME..."
# Определяем путь к файлу values
INGRESS_VALUES_FILE="$PROJECT_ROOT_DIR/k8s/ingress-nginx-values.yaml"

# Проверяем, существует ли файл values
if [[ ! -f "$INGRESS_VALUES_FILE" ]]; then
    error_exit "Файл значений Helm для ingress-nginx не найден: $INGRESS_VALUES_FILE"
fi

helm install $HELM_INGRESS_RELEASE_NAME $HELM_INGRESS_REPO_NAME/$HELM_INGRESS_CHART_NAME \
    --namespace $HELM_INGRESS_NAMESPACE \
    --create-namespace \
    -f "$INGRESS_VALUES_FILE" \
    --wait # Wait for Helm installation to complete

# Check deployment status explicitly after Helm wait (Helm wait isn't always perfect)
kwait "$HELM_INGRESS_NAMESPACE" "condition=Available" "deployment/${HELM_INGRESS_RELEASE_NAME}-controller" "180s"

# 🛢️ 6. Deploy Infrastructure (DB, Cache, MQ)
info "🛢 Установка PostgreSQL, Redis, Kafka..."
kapply_ns "$POSTGRES_MANIFEST"
kapply_ns "$REDIS_MANIFEST"
kapply_ns "$KAFKA_MANIFEST"

kwait "$NAMESPACE" "condition=Ready" "pod -l app=postgres" "120s"
kwait "$NAMESPACE" "condition=Ready" "pod -l app=redis" "120s"
kwait "$NAMESPACE" "condition=Ready" "pod/kafka-0" "180s" # Kafka might take longer

# 🚀 7. Deploy Application
info "🚀 Деплой приложения '$APP_NAME'..."
# Use envsubst to replace image tag in the template
export IMAGE_NAME=$IMAGE_NAME # Ensure variable is exported
if [[ ! -f "$APP_TEMPLATE_MANIFEST" ]]; then
    error_exit "Файл шаблона приложения не найден: $APP_TEMPLATE_MANIFEST"
fi
info "Applying $APP_TEMPLATE_MANIFEST with image $IMAGE_NAME..."
envsubst < "$APP_TEMPLATE_MANIFEST" | kubectl apply -n $NAMESPACE -f -

kwait "$NAMESPACE" "condition=Ready" "pod -l app=$APP_NAME" "180s" # Use variable app name

# 🌍 8. Apply Ingress Route
info "🌍 Применение ingress-маршрутов приложения..."
kapply_ns "$APP_INGRESS_MANIFEST"
sleep 5 # Allow ingress controller to pick up the new route

# 🧪 10. Health Check (using the dedicated script)
# Source the health check script. This will execute it in the current shell.
source "$PROJECT_ROOT_DIR/k8s/check-health.sh"

echo ""
echo "🎉 Деплой завершён!"