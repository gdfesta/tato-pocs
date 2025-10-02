#!/bin/bash

# PostgreSQL Database Creation Script
# Usage: ./create_database.sh <database_name> [username] [password]

set -e

# Check if database name is provided
if [ -z "$1" ]; then
    echo "Error: Database name is required"
    echo "Usage: $0 <database_name> [username] [password]"
    echo "Example: $0 my_app_db"
    echo "         $0 my_app_db my_user my_password"
    exit 1
fi

DB_NAME="$1"
DB_USER="${2:-${DB_NAME}_user}"
DB_PASSWORD="${3:-$(openssl rand -base64 12)}"

# PostgreSQL connection details (adjust if needed)
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

echo "Creating database: $DB_NAME"
echo "Creating user: $DB_USER"
echo "Generated password: $DB_PASSWORD"
echo ""

# Create database and user
PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres << EOF
-- Create user if not exists
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$DB_USER') THEN
        CREATE ROLE $DB_USER LOGIN PASSWORD '$DB_PASSWORD';
    END IF;
END
\$\$;

-- Create database if not exists
SELECT 'CREATE DATABASE $DB_NAME OWNER $DB_USER'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
EOF

echo ""
echo "✅ Database setup complete!"
echo ""
echo "Database Details:"
echo "  Name: $DB_NAME"
echo "  User: $DB_USER"
echo "  Password: $DB_PASSWORD"
echo "  Host: $POSTGRES_HOST"
echo "  Port: $POSTGRES_PORT"
echo ""
echo "Connection string:"
echo "  postgresql://$DB_USER:$DB_PASSWORD@$POSTGRES_HOST:$POSTGRES_PORT/$DB_NAME"
echo ""
echo "To connect via psql:"
echo "  PGPASSWORD='$DB_PASSWORD' psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $DB_USER -d $DB_NAME"