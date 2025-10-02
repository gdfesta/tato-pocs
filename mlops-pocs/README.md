# MLOps Proof of Concepts

A comprehensive collection of Machine Learning Operations (MLOps) examples and proof of concepts demonstrating the complete ML lifecycle using MLflow and cloud platforms.

## 📋 Overview

This repository contains practical implementations and examples for productionizing machine learning models, covering the entire ML lifecycle from experimentation to deployment. The project focuses on MLflow as the primary MLOps platform with integration examples for AWS SageMaker.

## 🏗️ Repository Structure

```
├── resumen.md                          # Comprehensive MLOps and MLflow theory guide
├── udemy-course/                       # Hands-on examples and implementations
│   ├── basic_examples/                 # MLflow fundamentals
│   ├── aws_example/                    # AWS SageMaker deployment
│   ├── mlproject_example/              # MLflow Projects structure
│   ├── docker-compose.yml              # Local MLflow server setup
│   └── requirements.txt                # Python dependencies
└── *.png                              # Architecture diagrams and visualizations
```

## 🚀 Key Features

### MLflow Components Covered
- **MLflow Tracking**: Experiment management and metrics logging
- **MLflow Models**: Model packaging and deployment
- **MLflow Registry**: Centralized model versioning and lifecycle management
- **MLflow Projects**: Reproducible ML code organization

### Practical Examples
- Basic MLflow tracking and logging
- PyCaret integration
- Custom metrics and artifacts
- Model registration and versioning
- Model validation and thresholds
- AWS SageMaker deployment pipeline

### Infrastructure
- Dockerized MLflow server with PostgreSQL backend
- MinIO for artifact storage (S3-compatible)
- Complete local development environment

## 🛠️ Quick Start

### Prerequisites
- Python 3.13+
- Docker and Docker Compose
- AWS CLI (for cloud examples)

### Local Setup
1. **Install dependencies:**
   ```bash
   pip install -r udemy-course/requirements.txt
   ```

2. **Start MLflow server:**
   ```bash
   cd udemy-course
   docker-compose up -d
   ```

3. **Access MLflow UI:**
   - MLflow: http://localhost:5001
   - MinIO Console: http://localhost:9001

### Environment Configuration
The project uses `direnv` for automatic environment variable loading. See [udemy-course/readme.md](udemy-course/readme.md) for detailed setup instructions.

## 📚 Learning Path

1. **Start with theory**: Read `resumen.md` for comprehensive MLOps concepts
2. **Basic examples**: Explore `udemy-course/basic_examples/` for MLflow fundamentals
3. **Project structure**: Study `udemy-course/mlproject_example/` for reproducible workflows
4. **Cloud deployment**: Follow `udemy-course/aws_example/` for production deployment

## 🎯 Use Cases

- **Data Scientists**: Learn MLOps best practices and MLflow usage
- **ML Engineers**: Understand model deployment and monitoring
- **DevOps Engineers**: Explore ML infrastructure and automation
- **Students**: Comprehensive MLOps learning resource

## 🏛️ Architecture

The repository demonstrates modern MLOps architecture patterns:
- Experiment tracking and versioning
- Model registry and lifecycle management
- Containerized deployment strategies
- Cloud-native ML pipelines
- Monitoring and maintenance workflows

## 📖 Documentation

- `resumen.md` - Complete MLOps and MLflow theory guide (Spanish)
- `udemy-course/readme.md` - Setup and usage instructions
- `udemy-course/aws_example/README.md` - AWS deployment guide

## 🔧 Technologies

- **MLflow**: Core MLOps platform
- **Docker**: Containerization
- **PostgreSQL**: Metadata storage
- **MinIO**: Artifact storage (S3-compatible)
- **AWS SageMaker**: Cloud deployment
- **PyCaret**: AutoML integration
- **XGBoost**: Model examples