#!/bin/bash
set -e

NAMESPACE="chatverse"
APP_SERVICE="chatverse-app"
NODE_PORT=$(kubectl get svc -n $NAMESPACE $APP_SERVICE -o jsonpath='{.spec.ports[0].nodePort}')
NODE_IP=$(hostname -I | awk '{print $1}')

echo "🔍 Checking application health at http://${NODE_IP}:${NODE_PORT}/actuator/health"
curl -sS "http://${NODE_IP}:${NODE_PORT}/actuator/health" | jq .