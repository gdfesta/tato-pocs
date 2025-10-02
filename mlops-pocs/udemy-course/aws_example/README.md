# MLflow SageMaker Deployment

This project demonstrates how to deploy machine learning models to AWS SageMaker using MLflow.

## Architecture Overview

The deployment process consists of two separate steps:

1. **Container Building**: Creates a Docker image with the runtime environment
2. **Model Deployment**: Deploys a specific trained model from MLflow registry

## Files Structure

- `MLproject` - Defines the MLflow project configuration
- `conda.yaml` - Specifies the Python environment and dependencies
- `train.py` - Model training script
- `deploy.py` - Model deployment configuration
- `build+command.txt` - Container build command
- `params.py` - Hyperparameter grids for different models
- Other `.py` files - Training utilities and data processing

## Container Building

```bash
mlflow sagemaker build-and-push-container --container xgb --env-manager conda
```

This command:
- Reads `MLproject` to understand project structure
- Uses `conda.yaml` to create the environment
- Packages all Python files in the directory
- Builds a Docker container with conda environment
- Pushes to ECR with tag `xgb`

**Important**: The container only provides the runtime environment (Python + dependencies). It does NOT contain any specific trained model.

## Model Deployment

The `deploy.py` script deploys a specific model:

```python
model_uri="s3://mlflow-project-artifacts/4/d2ad59e0241c4f6f9212ff7e22ca780a/artifacts/XGBRegressor"
```

When deploying with `flavor="python_function"`, MLflow:
1. Downloads the pre-trained model artifact from S3
2. Uses MLflow's built-in serving infrastructure
3. The model artifact is self-contained for inference

## Key Insight: Python Files vs Model Artifacts

**The Python training files are NOT needed in the container for standard model serving.**

The model artifact (`model_uri`) contains everything needed for inference. The Python files are only required for:
- Initial model training
- Custom inference logic with complex preprocessing/postprocessing
- Custom classes used during inference

For basic MLflow model serving, the serialized model artifact handles everything.

## Deployment Workflow

1. Train models and log to MLflow registry
2. Build generic container: `mlflow sagemaker build-and-push-container --container xgb --env-manager conda`
3. Deploy specific model: Update `model_uri` in `deploy.py` and run deployment
4. The container provides runtime environment, model comes from MLflow registry

## Configuration

Update `deploy.py` with your specific:
- `model_uri` - Path to your trained model in MLflow
- `execution_role_arn` - AWS IAM role for SageMaker
- `bucket_name` - S3 bucket for artifacts
- `image_url` - ECR image URL from container build
- `region_name` - AWS region