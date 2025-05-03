#!/bin/bash

# Скрипт для сохранения логов и describe для ресурса Kubernetes,
# который не перешел в ожидаемое состояние.

set -e # Exit immediately if a command exits with a non-zero status.
set -u # Treat unset variables as an error.

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

# --- Argument Parsing ---
if [[ $# -lt 3 ]]; then
    echo "Usage: $0 <namespace> <resource_arg> <condition>"
    echo "  <namespace>: Kubernetes namespace (use '.' for cluster-wide resources if applicable)"
    echo "  <resource_arg>: Resource identifier (e.g., 'pod/my-pod', 'pod -l app=my-app', 'deployment/my-deploy')"
    echo "  <condition>: The condition that failed (e.g., 'condition=Ready', 'condition=Available')"
    exit 1
fi

NAMESPACE="$1"
RESOURCE_ARG="$2"
FAILED_CONDITION="$3" # Используем для сообщения об ошибке

PROJECT_ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$PROJECT_ROOT_DIR/logs" # Define logs directory path

# Create logs directory if it doesn't exist
mkdir -p "$LOG_DIR"

# --- Main Logic ---
timestamp=$(date +%Y%m%d-%H%M%S)
ns_arg=""
if [[ "$NAMESPACE" != "." ]]; then # Handle potential cluster-wide resources
    ns_arg="-n $NAMESPACE"
fi

echo "❌ Resource '$RESOURCE_ARG' in namespace '$NAMESPACE' failed condition '$FAILED_CONDITION'." >&2
echo "--- Saving diagnostic information to '$LOG_DIR' ---" >&2

# Determine if it's a label selector or specific resource
if [[ "$RESOURCE_ARG" == *"-l "* ]]; then
    label_selector=$(echo "$RESOURCE_ARG" | sed 's/.*-l //')
    safe_label_selector=$(echo "$label_selector" | sed 's/[^a-zA-Z0-9]/-/g')
    resource_type="pod" # Assume pod for label selector

    pod_names=$(kubectl get pods $ns_arg -l "$label_selector" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null)
    if [[ -n "$pod_names" ]]; then
        for pod_name in $pod_names; do
            # Always save logs/describe for all pods matching the label when the wait fails
            log_file="${LOG_DIR}/${pod_name}-${timestamp}.log"
            describe_file="${LOG_DIR}/${pod_name}-${timestamp}.describe"
            echo "--- Saving logs for pod: $pod_name to $log_file ---" >&2
            kubectl logs $ns_arg "$pod_name" --tail=100 > "$log_file" 2>&1 || echo "    (Could not fetch logs for $pod_name)" >> "$log_file"
            echo "--- Saving describe for pod: $pod_name to $describe_file ---" >&2
            kubectl describe pod $ns_arg "$pod_name" > "$describe_file" 2>&1 || echo "    (Could not describe pod $pod_name)" >> "$describe_file"
        done
    else
        echo "    (No pods found with label '$label_selector' to fetch logs/describe from)" >&2
    fi
else
   # Specific resource name
   resource_type=$(echo "$RESOURCE_ARG" | cut -d'/' -f1)
   resource_name=$(echo "$RESOURCE_ARG" | cut -d'/' -f2)
   safe_resource_name=$(echo "$resource_name" | sed 's/[^a-zA-Z0-9]/-/g')
   describe_file="${LOG_DIR}/${resource_type}-${safe_resource_name}-${timestamp}.describe"

   if [[ "$resource_type" == "pod" ]]; then
       log_file="${LOG_DIR}/${safe_resource_name}-${timestamp}.log"
       echo "--- Saving logs for pod: $resource_name to $log_file ---" >&2
       kubectl logs $ns_arg "$resource_name" --tail=100 > "$log_file" 2>&1 || echo "    (Could not fetch logs for $resource_name)" >> "$log_file"
       echo "--- Saving describe for pod: $resource_name to $describe_file ---" >&2
       kubectl describe pod $ns_arg "$resource_name" > "$describe_file" 2>&1 || echo "    (Could not describe pod $resource_name)" >> "$describe_file"
   else
       # For non-pod resources, just save describe output
       echo "--- Saving describe for resource: $RESOURCE_ARG to $describe_file ---" >&2
       kubectl describe "$resource_type" "$resource_name" $ns_arg > "$describe_file" 2>&1 || echo "    (Could not describe $RESOURCE_ARG)" >> "$describe_file"
   fi
fi

echo "✅ Diagnostic information saved to '$LOG_DIR'."
# Не используем exit 1 здесь, так как предполагается, что родительский скрипт уже завершится из-за неудачи wait