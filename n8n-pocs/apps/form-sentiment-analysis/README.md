# Form Sentiment Analysis

A web form application that collects user reviews and performs sentiment analysis using AI.

## Overview

This application uses n8n workflows to:
1. Collect user feedback through a web form (Name + Review)
2. Analyze sentiment using Ollama's deepseek-r1:1.5b model
3. Store results in PostgreSQL database

## Setup

1. **Start Infrastructure**
   ```bash
   cd ../../infra && docker-compose up -d
   ```

2. **Create Database and Run Migrations**
   ```bash
   cd apps/form-sentiment-analysis
   ./scripts/migrate.sh
   ```
   This will automatically:
   - Create the database if it doesn't exist
   - Download Flyway (if needed)
   - Run all database migrations

3. **Setup Qdrant Vector Database**
   ```bash
   ./scripts/setup-qdrant.sh
   ```
   This will automatically:
   - Create collections for storing review embeddings
   - Configure vector similarity search
   - Verify connectivity to Qdrant

## Database Schema

**PostgreSQL:**
- `form_submissions` - Stores form data and sentiment analysis results

**Qdrant Collections:**
- `reviews` - Stores review text embeddings for semantic search

## Development

### Database Migrations

- Add new migrations: `db/migrations/V{version}__{description}.sql`
- Run migrations: `./scripts/migrate.sh migrate`
- Check status: `./scripts/migrate.sh info`

### Configuration

- Database settings: `flyway.conf`
- Migration script: `scripts/migrate.sh`

## Usage

1. Access n8n at http://localhost:5678
2. Import the workflow from `workflows/sentiment-analysis.json`
3. Configure services in n8n:

### n8n Configuration

**Ollama Chat Model Node:**
- Base URL: `http://ollama:11434`
- Model: `deepseek-r1:1.5b`

**Qdrant Vector Database:**
- Host: `qdrant`
- Port: `6333`
- URL: `http://qdrant:6333`

**PostgreSQL Database Connections:**
- Host: `postgres`
- Port: `5432`
- database: `postgresql://form_sentiment_analysis_user:YOUR_PASSWORD@postgres:5432/form_sentiment_analysis`

**Important:** Use service names (`postgres`, `ollama`, `qdrant`) not `localhost` when configuring n8n since it runs inside Docker.

4. Test the form through the n8n webhook URL