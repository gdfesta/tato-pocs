#!/bin/bash

set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Calculate project root (two levels up from k8s/scripts/)
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# Parse arguments
DELETE_NAMESPACE=false
if [[ "$1" == "--delete-namespace" ]] || [[ "$1" == "-d" ]]; then
    DELETE_NAMESPACE=true
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🗑️  Undeploying Quarkus-Pekko Application from Kubernetes${NC}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl is not installed or not in PATH${NC}"
    exit 1
fi

# Check if namespace exists
if ! kubectl get namespace quarkus-pekko &> /dev/null; then
    echo -e "${YELLOW}⚠️  Namespace 'quarkus-pekko' does not exist. Nothing to undeploy.${NC}"
    exit 0
fi

echo -e "${YELLOW}🛑 Stopping all services...${NC}"

# Delete application components
echo -e "${YELLOW}  Removing application...${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/app-service.yaml" --ignore-not-found=true
kubectl delete -f "$PROJECT_ROOT/k8s/app-deployment.yaml" --ignore-not-found=true
kubectl delete -f "$PROJECT_ROOT/k8s/app-configmap.yaml" --ignore-not-found=true

echo -e "${YELLOW}  Removing PostgreSQL (keeping persistent data)...${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/postgresql.yaml" --ignore-not-found=true
# Note: PVC is NOT deleted to preserve data across deployments

echo -e "${YELLOW}  Removing Kafka and Zookeeper...${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/kafka.yaml" --ignore-not-found=true

echo -e "${YELLOW}  Removing AKHQ (Kafka UI)...${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/akhq.yaml" --ignore-not-found=true

# Delete RBAC
echo -e "${YELLOW}  Removing RBAC...${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/rbac.yaml" --ignore-not-found=true

# Delete namespace if requested
if [ "$DELETE_NAMESPACE" = true ]; then
    echo -e "${YELLOW}  Deleting namespace (this will remove all persistent data)...${NC}"
    kubectl delete namespace quarkus-pekko
    echo -e "${GREEN}✅ Namespace deleted${NC}"
else
    echo -e "${GREEN}✅ Namespace preserved (persistent data kept)${NC}"
fi

echo -e "${GREEN}🎉 Undeployment completed!${NC}"

# Check remaining resources
if kubectl get namespace quarkus-pekko &> /dev/null; then
    echo -e "${BLUE}📊 Remaining resources in namespace:${NC}"
    kubectl get all -n quarkus-pekko
else
    echo -e "${GREEN}🧹 All resources have been cleaned up${NC}"
fi