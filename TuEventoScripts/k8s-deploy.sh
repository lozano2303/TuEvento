#!/bin/bash

# TuEvento - Kubernetes Setup Script
# Este script inicializa el entorno Kubernetes para despliegue

set -e

echo "========================================="
echo "TuEvento Kubernetes Deployment Script"
echo "========================================="

# Configurar variables
NAMESPACE="tuevento"
K8S_DIR="./k8s"

# Crear namespace
echo "Creating namespace: $NAMESPACE"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Aplicar secrets primero
echo "Applying secrets..."
kubectl apply -f $K8S_DIR/secrets.yaml -n $NAMESPACE

# Aplicar configmap
echo "Applying configmap..."
kubectl apply -f $K8S_DIR/configmap.yaml -n $NAMESPACE

# Aplicar deployments
echo "Applying backend deployment..."
kubectl apply -f $K8S_DIR/deployment.yaml -n $NAMESPACE

echo "Applying frontend deployment..."
kubectl apply -f $K8S_DIR/frontend-deployment.yaml -n $NAMESPACE

# Aplicar services
echo "Applying services..."
kubectl apply -f $K8S_DIR/service.yaml -n $NAMESPACE
kubectl apply -f $K8S_DIR/frontend-service.yaml -n $NAMESPACE

# Aplicar HPA (horizontal pod autoscaler)
echo "Applying HPA..."
kubectl apply -f $K8S_DIR/hpa.yaml -n $NAMESPACE

# Aplicar ingress (opcional, requiere ingress controller)
echo "Applying ingress..."
kubectl apply -f $K8S_DIR/ingress.yaml -n $NAMESPACE 2>/dev/null || echo "Ingress not applied (may require ingress controller)"

# Esperar a que los pods estén listos
echo "Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app=tu-evento-backend -n $NAMESPACE --timeout=120s
kubectl wait --for=condition=ready pod -l app=tu-evento-frontend -n $NAMESPACE --timeout=120s || true

# Mostrar estado
echo ""
echo "========================================="
echo "Deployment Complete!"
echo "========================================="
echo ""
kubectl get pods -n $NAMESPACE
echo ""
kubectl get services -n $NAMESPACE
echo ""
echo "Access URLs:"
echo "  Frontend: http://localhost:30081"
echo "  Backend:  http://localhost:30080"
echo "  API:      http://localhost:30080/api"
echo ""