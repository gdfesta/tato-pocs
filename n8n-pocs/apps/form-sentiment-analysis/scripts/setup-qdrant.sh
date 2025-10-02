#!/bin/bash

# Qdrant Setup Script for Form Sentiment Analysis

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Qdrant connection details
QDRANT_HOST="${QDRANT_HOST:-localhost}"
QDRANT_PORT="${QDRANT_PORT:-6333}"
QDRANT_URL="http://$QDRANT_HOST:$QDRANT_PORT"

cd "$APP_DIR"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed"
    log_error "Install it with: sudo apt install jq"
    exit 1
fi

# Check if Qdrant is running
log_info "Checking Qdrant connectivity..."
if ! curl -s "$QDRANT_URL" > /dev/null; then
    log_error "Qdrant is not accessible at $QDRANT_URL"
    log_error "Make sure Qdrant is running: cd ../../infra && docker-compose up -d"
    exit 1
fi

log_info "Qdrant is running at $QDRANT_URL"

# Read collections configuration
COLLECTIONS_FILE="$APP_DIR/qdrant/collections.json"

if [ ! -f "$COLLECTIONS_FILE" ]; then
    log_error "Collections file not found: $COLLECTIONS_FILE"
    exit 1
fi

# Function to create collection
create_collection() {
    local collection_name="$1"
    local collection_config="$2"

    log_info "Creating collection: $collection_name"

    # Check if collection already exists
    if curl -s "$QDRANT_URL/collections/$collection_name" | grep -q "\"status\":\"ok\""; then
        log_warn "Collection '$collection_name' already exists"
        return 0
    fi

    # Create collection
    local response=$(curl -s -X PUT "$QDRANT_URL/collections/$collection_name" \
        -H "Content-Type: application/json" \
        -d "$collection_config")

    if echo "$response" | grep -q "\"status\":\"ok\""; then
        log_info "✅ Collection '$collection_name' created successfully"
    else
        log_error "❌ Failed to create collection '$collection_name'"
        echo "Response: $response"
        return 1
    fi
}

# Function to get collection info
get_collection_info() {
    local collection_name="$1"

    log_info "Collection '$collection_name' info:"
    curl -s "$QDRANT_URL/collections/$collection_name" | jq '.'
}

# Parse and create collections from JSON
log_info "Setting up Qdrant collections..."

# Use jq for JSON parsing
for collection_name in $(cat "$COLLECTIONS_FILE" | jq -r 'keys[]'); do
    collection_config=$(cat "$COLLECTIONS_FILE" | jq ".$collection_name")
    create_collection "$collection_name" "$collection_config"
done

# Show collection status
log_info "Listing all collections:"
curl -s "$QDRANT_URL/collections" | jq '.'

echo ""
log_info "✅ Qdrant setup completed successfully!"
echo ""
log_info "Access Qdrant:"
log_info "  Dashboard: http://$QDRANT_HOST:$QDRANT_PORT/dashboard"
log_info "  API: $QDRANT_URL"
echo ""
log_info "From n8n, use: http://qdrant:6333"