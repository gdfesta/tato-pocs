# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a monorepo for AI automation applications built during the Udemy course "AI Automation: Build LLM Apps & AI Agents with n8n APIs". Each application is organized in its own folder using kebab-case naming convention.

## Project Structure

**Root Level:**
- `.env` - Environment variables including OpenAI API key
- `Important+Links.pdf` - Course documentation and reference links
- `README.md` - Project information and monorepo structure

**Folders:**
- `apps/` - Individual applications (each in kebab-case folders)
  - `apps/form-sentiment-analysis/` - Form sentiment analysis application
- `scripts/` - Shared utility scripts
  - `scripts/create_database.sh` - Script to create PostgreSQL databases for each app
- `infra/` - Infrastructure and Docker setup
  - `infra/docker-compose.yml` - Docker Compose configuration for n8n, PostgreSQL, Ollama, and Qdrant
  - `infra/ollama-entrypoint.sh` - Ollama initialization script for Docker
  - `infra/init-db.sql` - PostgreSQL initialization script

**Naming Convention:**
- All application folders use kebab-case (lowercase with hyphens)
- Each app should have its own database created via the `create_database.sh` script

## Environment Configuration

The `.env` file contains:
- `OPEN_API_KEY` - OpenAI API key for LLM integrations (note: typo in variable name, should be `OPENAI_API_KEY`)

## Local Development Setup

Use Docker Compose to run the full stack locally:
- `cd infra && docker-compose up -d` - Start all services
- Access services:
  - n8n: http://localhost:5678 (admin/admin)
  - Ollama: http://localhost:11434
  - Qdrant: http://localhost:6333 (vector database)
  - PostgreSQL: localhost:5432
- `cd infra && docker-compose down` - Stop services (data persists)

## Course Context

This project follows the Udemy course "AI Automation: Build LLM Apps & AI Agents with n8n APIs" and demonstrates practical implementations of:
- Workflow automation patterns
- External API integrations
- LLM-powered automation scenarios