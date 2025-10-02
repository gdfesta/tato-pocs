#!/bin/bash

# Flyway Migration Script for Form Sentiment Analysis

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FLYWAY_VERSION="10.19.0"
FLYWAY_DIR="$APP_DIR/.flyway"
FLYWAY_BIN="$FLYWAY_DIR/flyway-$FLYWAY_VERSION/flyway"

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

# Download Flyway if not exists
if [ ! -f "$FLYWAY_BIN" ]; then
    log_info "Downloading Flyway $FLYWAY_VERSION..."
    mkdir -p "$FLYWAY_DIR"
    cd "$FLYWAY_DIR"

    FLYWAY_URL="https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/$FLYWAY_VERSION/flyway-commandline-$FLYWAY_VERSION-linux-x64.tar.gz"
    curl -L "$FLYWAY_URL" | tar xz

    chmod +x "$FLYWAY_BIN"
    log_info "Flyway downloaded successfully"
fi

cd "$APP_DIR"

# Get password from user if not set in environment
if [ -z "$FLYWAY_PASSWORD" ]; then
    echo -n "Enter database password for form_sentiment_analysis_user: "
    read -s FLYWAY_PASSWORD
    echo
fi

# Check if database exists
DB_NAME="form_sentiment_analysis"
if ! PGPASSWORD=postgres psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
    log_warn "Database $DB_NAME does not exist. Creating it now..."
    ../../scripts/create_database.sh $DB_NAME form_sentiment_analysis_user $FLYWAY_PASSWORD

    # Update flyway.conf with the password
    sed -i "s/flyway.password=.*/flyway.password=$FLYWAY_PASSWORD/" flyway.conf
    log_info "Updated flyway.conf with database password"
else
    log_info "Database $DB_NAME already exists"
    # Read password from flyway.conf if not provided
    if [ -z "$FLYWAY_PASSWORD" ]; then
        FLYWAY_PASSWORD=$(grep "flyway.password=" flyway.conf | cut -d'=' -f2)
        log_info "Using password from flyway.conf"
    fi
fi

# Run migration
log_info "Running Flyway migration..."
if [ $# -eq 0 ]; then
    # Default to migrate if no command provided
    "$FLYWAY_BIN" -configFiles=flyway.conf migrate
else
    # Use provided command
    "$FLYWAY_BIN" -configFiles=flyway.conf "$@"
fi

log_info "Migration completed successfully!"