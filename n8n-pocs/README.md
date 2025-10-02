# AI Automation Udemy Course

This is a monorepo for building AI automation applications using n8n and APIs. Each application is organized in its own folder using kebab-case naming convention.

Course link: https://intive-learning.udemy.com/course/ai-automation-build-llm-apps-ai-agents-with-n8n-apis/learn/lecture/48684429#overview

## Applications

- `apps/form-sentiment-analysis/` - Form sentiment analysis application

## Project Structure

- `apps/` - Individual applications (each in kebab-case folders)
- `scripts/` - Shared utility scripts
- `infra/` - Infrastructure and Docker setup

## Prerequisites

- Docker and Docker Compose (for n8n and PostgreSQL)
- PostgreSQL client tools: `sudo apt install postgresql-client`
- jq (JSON processor): `sudo apt install jq`

## Getting Started

### Start n8n with Docker Compose

1. Start the services:
   ```bash
   cd infra && docker-compose up -d
   ```

2. Access services:
   - **n8n**: http://localhost:5678 (admin/admin)
   - **Ollama**: http://localhost:11434
   - **Qdrant**: http://localhost:6333/dashboard#/collections (vector database)

3. Stop the services:
   ```bash
   cd infra && docker-compose down
   ```

### Ollama Setup

The Deepseek-R1 1.5B model will be automatically downloaded when you first start the services.

Test the model:
```bash
curl http://localhost:11434/api/generate -d '{
  "model": "deepseek-r1:1.5b",
  "prompt": "Hello, how are you?",
  "stream": false
}'
```

### Database Access

Connect to PostgreSQL from DBeaver or other database tools:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `n8n`
- **Username**: `n8n`
- **Password**: `n8n_password`

