#!/bin/bash

set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Calculate project root (two levels up from k8s/scripts/)
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Deploying Quarkus-Pekko Application to Local Kubernetes${NC}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl is not installed or not in PATH${NC}"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

# Check if Kubernetes is enabled in Docker Desktop
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}❌ Kubernetes is not running. Please enable Kubernetes in Docker Desktop.${NC}"
    exit 1
fi

echo -e "${YELLOW}📋 Prerequisites check passed${NC}"

# Clean previous container images
echo -e "${BLUE}🧹 Cleaning previous container images...${NC}"
docker images --format "table {{.Repository}}:{{.Tag}}" | grep "com.gdfesta.example/quarkus-pekko" | xargs -r docker rmi 2>/dev/null || true

echo -e "${BLUE}🔨 Building application and container image...${NC}"
"$PROJECT_ROOT/mvnw" clean package -Dquarkus.container-image.build=true -Dquarkus.profile=k8s -Dquarkus.container-image.tag=1.0.0-SNAPSHOT

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Application built successfully${NC}"

# Deploy to Kubernetes
echo -e "${BLUE}🚢 Deploying to Kubernetes...${NC}"

# Create namespace
echo -e "${YELLOW}📦 Creating namespace...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/namespace.yaml"

# Deploy RBAC
echo -e "${YELLOW}🔐 Setting up RBAC...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/rbac.yaml"

# Deploy PostgreSQL PVC (persistent storage)
echo -e "${YELLOW}💾 Creating persistent storage for PostgreSQL...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/postgresql-pvc.yaml"

# Deploy PostgreSQL
echo -e "${YELLOW}🐘 Deploying PostgreSQL...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/postgresql.yaml"

# Wait for PostgreSQL to be ready before deploying the application
echo -e "${BLUE}⏳ Waiting for PostgreSQL to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/postgres -n quarkus-pekko

echo -e "${GREEN}✅ PostgreSQL is ready!${NC}"

# Deploy Kafka and Zookeeper
echo -e "${YELLOW}📨 Deploying Kafka and Zookeeper...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/kafka.yaml"

# Wait for Kafka to be ready
echo -e "${BLUE}⏳ Waiting for Zookeeper and Kafka to be ready...${NC}"
kubectl wait --for=jsonpath='{.status.readyReplicas}'=1 --timeout=300s statefulset/zookeeper -n quarkus-pekko
kubectl wait --for=jsonpath='{.status.readyReplicas}'=1 --timeout=300s statefulset/kafka -n quarkus-pekko

echo -e "${GREEN}✅ Kafka is ready!${NC}"

# Deploy AKHQ (Kafka UI)
echo -e "${YELLOW}🖥️  Deploying AKHQ (Kafka UI)...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/akhq.yaml"

# Deploy application configuration
echo -e "${YELLOW}⚙️  Deploying application configuration...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/app-configmap.yaml"

# Deploy application
echo -e "${YELLOW}🚀 Deploying application...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/app-deployment.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/app-service.yaml"

echo -e "${GREEN}✅ Deployment completed successfully!${NC}"

# Wait for application to be ready
echo -e "${BLUE}⏳ Waiting for application to be ready...${NC}"

echo -e "${YELLOW}  Waiting for application...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/quarkus-pekko -n quarkus-pekko

echo -e "${GREEN}🎉 All deployments are ready!${NC}"

# Show deployment status
echo -e "${BLUE}📊 Deployment Status:${NC}"
kubectl get pods -n quarkus-pekko

echo ""
echo -e "${GREEN}🌐 Access the application:${NC}"
echo -e "  Port forward: ${YELLOW}kubectl port-forward svc/quarkus-pekko 8080:8080 -n quarkus-pekko${NC}"
echo -e "  Then visit: ${YELLOW}http://localhost:8080${NC}"
echo ""
echo -e "${GREEN}📝 Useful commands:${NC}"
echo -e "  View logs: ${YELLOW}kubectl logs -f deployment/quarkus-pekko -n quarkus-pekko${NC}"
echo -e "  Scale app: ${YELLOW}kubectl scale deployment/quarkus-pekko --replicas=5 -n quarkus-pekko${NC}"
echo -e "  Delete all: ${YELLOW}./undeploy.sh${NC}"