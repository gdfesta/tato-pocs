# MLflow Project

## Setup
> conda create -n mlops_udemy python=3.13
> pip install -r requirements.txt

## Environment Variables (direnv)
This project uses `direnv` to automatically load environment variables from `.env` file.

### Required Files
Create these files in the project root (they are git-ignored):

#### `.env`
```bash
MLFLOW_PORT=5001
MLFLOW_HOST=0.0.0.0
MLFLOW_TRACKING_URI=http://localhost:5001

# Database Configuration
POSTGRES_DB=mlflow
POSTGRES_USER=mlflow
POSTGRES_PASSWORD=mlflow123
DATABASE_URL=postgresql://mlflow:mlflow123@localhost:5432/mlflow

# MinIO/S3 Configuration (for external Python scripts)
AWS_ACCESS_KEY_ID=minioadmin
AWS_SECRET_ACCESS_KEY=minioadmin
MLFLOW_S3_ENDPOINT_URL=http://localhost:9000
```

#### `.envrc`
```bash
#!/usr/bin/env bash

# Load environment variables from .env file
dotenv .env

# Add any additional direnv configuration here if needed
```

### Install direnv
```bash
# Ubuntu/Debian
sudo apt install direnv

# macOS
brew install direnv

# Add to your shell (bash/zsh)
echo 'eval "$(direnv hook bash)"' >> ~/.bashrc  # for bash
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc    # for zsh
```

### Setup
```bash
# Allow direnv to load .env file
direnv allow

# Environment variables will be automatically loaded when you cd into this directory
# and unloaded when you leave
```

## Running MLflow

### Docker Compose (Server + UI)
```bash
# Linux
docker-compose up -d

# macOS
docker compose up -d
```

**Note**: Docker Compose automatically reads `.env` file for variable substitution, so no additional setup needed.

**Port Configuration**: We use port 5001 on the host because port 5000 is commonly used by macOS AirPlay Receiver (ControlCenter). The MLflow server runs on port 5000 inside the Docker container, but is mapped to host port 5001. Note that Docker logs will show the server running on port 5000 (internal container port).

Access MLflow UI at: http://localhost:5001

**Note**: The required S3 bucket (`mlflow-artifacts`) is automatically created during startup.

**Services:**
- MLflow UI: http://localhost:5001
- MinIO Console: http://localhost:9001 (user: minioadmin, pass: minioadmin)
- PostgreSQL: localhost:5432

### Docker Commands
```bash
# Start services
docker-compose up -d    # Linux
docker compose up -d    # macOS

# View logs
docker-compose logs -f  # Linux
docker compose logs -f  # macOS

# Tail specific service logs
docker logs -f mlflow-tracking-server
docker logs -f --tail 20 mlflow-tracking-server

# Stop services
docker-compose down     # Linux
docker compose down     # macOS

# Remove all data and start fresh (deletes all experiments, runs, and artifacts)
docker-compose down -v  # Linux
docker compose down -v  # macOS
```