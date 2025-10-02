-- Initialize PostgreSQL with n8n database and user
-- This runs automatically when the container starts for the first time

-- Create n8n user
CREATE USER n8n WITH PASSWORD 'n8n_password';

-- Create n8n database
CREATE DATABASE n8n OWNER n8n;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n;

-- Connect to n8n database and grant schema privileges
\c n8n;
GRANT ALL ON SCHEMA public TO n8n;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO n8n;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO n8n;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO n8n;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO n8n;