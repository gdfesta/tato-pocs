#!/bin/bash

set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔄 Redeploying Quarkus-Pekko Application${NC}"
echo ""

# Step 1: Undeploy existing resources (preserve namespace and persistent data)
echo -e "${YELLOW}Step 1/3: Undeploying existing resources...${NC}"
"$SCRIPT_DIR/undeploy.sh"
echo ""

# Step 2: Deploy the application
echo -e "${YELLOW}Step 2/3: Deploying application...${NC}"
"$SCRIPT_DIR/deploy.sh"
echo ""

# Step 3: Port forward all services
echo -e "${YELLOW}Step 3/3: Setting up port forwarding...${NC}"
"$SCRIPT_DIR/port-forward.sh" all