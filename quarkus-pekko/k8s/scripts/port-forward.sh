#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🌐 Port Forward Setup for Quarkus-Pekko${NC}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl is not installed or not in PATH${NC}"
    exit 1
fi

# Check if namespace exists
if ! kubectl get namespace quarkus-pekko &> /dev/null; then
    echo -e "${RED}❌ Namespace 'quarkus-pekko' does not exist. Deploy the application first.${NC}"
    exit 1
fi

# Function to start port forwarding
start_port_forward() {
    local service=$1
    local local_port=$2
    local remote_port=$3
    local description=$4

    echo -e "${YELLOW}🔌 Starting port forward for $description...${NC}"
    echo -e "   ${GREEN}Local: http://localhost:$local_port${NC}"

    # Start port forward in background
    kubectl port-forward svc/$service $local_port:$remote_port -n quarkus-pekko &
    local pid=$!

    # Store PID for cleanup
    echo $pid >> /tmp/k8s-port-forwards.pid

    # Give it a moment to start
    sleep 2

    if kill -0 $pid 2>/dev/null; then
        echo -e "   ${GREEN}✅ Port forward active (PID: $pid)${NC}"
        return 0
    else
        echo -e "   ${RED}❌ Failed to start port forward${NC}"
        return 1
    fi
}

# Function to stop all port forwards
stop_port_forwards() {
    echo -e "${YELLOW}🛑 Stopping all port forwards...${NC}"

    if [ -f /tmp/k8s-port-forwards.pid ]; then
        while read pid; do
            if kill -0 $pid 2>/dev/null; then
                kill $pid
                echo -e "   ${GREEN}✅ Stopped port forward (PID: $pid)${NC}"
            fi
        done < /tmp/k8s-port-forwards.pid
        rm -f /tmp/k8s-port-forwards.pid
    else
        echo -e "   ${YELLOW}ℹ️  No active port forwards found${NC}"
    fi

    # Also kill any kubectl port-forward processes
    pkill -f "kubectl port-forward" 2>/dev/null || true

    echo -e "${GREEN}🎉 All port forwards stopped${NC}"
}

# Function to show status
show_status() {
    echo -e "${BLUE}📊 Current Status:${NC}"
    echo ""

    # Check pods
    echo -e "${YELLOW}Pods:${NC}"
    kubectl get pods -n quarkus-pekko
    echo ""

    # Check services
    echo -e "${YELLOW}Services:${NC}"
    kubectl get svc -n quarkus-pekko
    echo ""

    # Check active port forwards
    echo -e "${YELLOW}Active Port Forwards:${NC}"
    if pgrep -f "kubectl port-forward" > /dev/null; then
        ps aux | grep "kubectl port-forward" | grep -v grep
    else
        echo -e "   ${YELLOW}ℹ️  No active port forwards${NC}"
    fi
}

# Parse command line arguments
case "${1:-}" in
    "app"|"application")
        start_port_forward "quarkus-pekko" "8080" "8080" "Application (REST API)"
        echo -e "${GREEN}🚀 Application available at: http://localhost:8080${NC}"
        echo -e "${GREEN}📊 Health check: http://localhost:8080/q/health${NC}"
        echo -e "${GREEN}🔧 Try: curl http://localhost:8080/greetings/count${NC}"
        ;;
    "management"|"pekko")
        start_port_forward "quarkus-pekko" "7626" "7626" "Pekko Management"
        echo -e "${GREEN}⚙️  Pekko Management available at: http://localhost:7626${NC}"
        echo -e "${GREEN}🔧 Try: curl http://localhost:7626/ready${NC}"
        ;;
    "postgres"|"db")
        start_port_forward "postgres" "5432" "5432" "PostgreSQL Database"
        echo -e "${GREEN}🐘 PostgreSQL available at: localhost:5432${NC}"
        echo -e "${GREEN}🔧 Connect: psql -h localhost -p 5432 -U quarkus -d quarkus${NC}"
        ;;
    "akhq"|"kafka-ui")
        start_port_forward "akhq" "8081" "8080" "AKHQ (Kafka UI)"
        echo -e "${GREEN}🖥️  AKHQ (Kafka UI) available at: http://localhost:8081${NC}"
        echo -e "${GREEN}📊 View topics, messages, and consumer groups${NC}"
        ;;
    "all")
        echo -e "${BLUE}🌐 Setting up all port forwards...${NC}"
        echo ""

        # Clear any existing PID file
        rm -f /tmp/k8s-port-forwards.pid

        start_port_forward "quarkus-pekko" "8080" "8080" "Application (REST API)"
        start_port_forward "quarkus-pekko" "7626" "7626" "Pekko Management"
        start_port_forward "postgres" "5432" "5432" "PostgreSQL Database"
        start_port_forward "akhq" "8081" "8080" "AKHQ (Kafka UI)"

        echo ""
        echo -e "${GREEN}🎉 All services are now accessible locally:${NC}"
        echo -e "   ${YELLOW}Application:${NC}     http://localhost:8080"
        echo -e "   ${YELLOW}Pekko Management:${NC} http://localhost:7626"
        echo -e "   ${YELLOW}PostgreSQL:${NC}      localhost:5432"
        echo -e "   ${YELLOW}AKHQ (Kafka UI):${NC} http://localhost:8081"
        echo ""
        echo -e "${BLUE}💡 Tip: Use '$0 stop' to stop all port forwards${NC}"

        # Wait for user to stop
        echo -e "${YELLOW}Press Ctrl+C to stop all port forwards...${NC}"
        trap stop_port_forwards EXIT
        wait
        ;;
    "stop")
        stop_port_forwards
        ;;
    "status")
        show_status
        ;;
    *)
        echo -e "${GREEN}📚 Usage: $0 [service|command]${NC}"
        echo ""
        echo -e "${YELLOW}Services:${NC}"
        echo -e "  ${BLUE}app${NC} or ${BLUE}application${NC}   - Forward application port (8080)"
        echo -e "  ${BLUE}management${NC} or ${BLUE}pekko${NC}   - Forward Pekko management port (7626)"
        echo -e "  ${BLUE}postgres${NC} or ${BLUE}db${NC}        - Forward PostgreSQL port (5432)"
        echo -e "  ${BLUE}akhq${NC} or ${BLUE}kafka-ui${NC}      - Forward AKHQ/Kafka UI port (8081)"
        echo -e "  ${BLUE}all${NC}                   - Forward all ports (blocks until Ctrl+C)"
        echo ""
        echo -e "${YELLOW}Commands:${NC}"
        echo -e "  ${BLUE}stop${NC}                  - Stop all active port forwards"
        echo -e "  ${BLUE}status${NC}                - Show deployment status"
        echo ""
        echo -e "${YELLOW}Examples:${NC}"
        echo -e "  ${GREEN}$0 app${NC}                - Access application at http://localhost:8080"
        echo -e "  ${GREEN}$0 akhq${NC}               - Access Kafka UI at http://localhost:8081"
        echo -e "  ${GREEN}$0 all${NC}                - Forward all ports (interactive)"
        echo -e "  ${GREEN}$0 stop${NC}               - Stop all port forwards"
        echo ""
        show_status
        ;;
esac