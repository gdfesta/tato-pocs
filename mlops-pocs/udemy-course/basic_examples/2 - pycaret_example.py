import os
import pandas as pd
from pycaret.datasets import get_data
from pycaret.classification import setup, compare_models, ensemble_model, tune_model, finalize_model
import mlflow
import mlflow.sklearn

# Load sample data
diabetes = get_data('diabetes')

tracking_uri = os.getenv("MLFLOW_TRACKING_URI", "http://localhost:5001")
mlflow.set_tracking_uri(uri=tracking_uri)

# Set up MLflow experiment
mlflow.set_experiment("pycaret_automl_experiment")

# Setup PyCaret environment with MLflow logging enabled
clf = setup(
    data=diabetes,
    target='Class variable',
    log_experiment=True,  # Enable MLflow logging
    experiment_name='pycaret_automl_experiment',  # Match MLflow experiment name
    log_plots=True,  # Log plots to MLflow
    log_data=True    # Log data info to MLflow
)

# Compare multiple models (AutoML)
best_models = compare_models(
    include=['rf', 'lr', 'dt', 'nb'],  # Using commonly available models
    sort='Accuracy',
    n_select=3  # Select top 3 models
)

# Create ensemble of best models
ensemble = ensemble_model(best_models[0])

# Tune hyperparameters
tuned_model = tune_model(ensemble)

# Finalize model (trains on full dataset)
final_model = finalize_model(tuned_model)

# Log additional metrics and tags
mlflow.log_param("models_compared", len(['rf', 'lr', 'dt', 'nb']))
mlflow.log_param("ensemble_method", "voting")
mlflow.log_param("dataset_name", "diabetes")
mlflow.log_param("target_variable", "Class variable")

# Add tags for better organization
mlflow.set_tag("framework", "PyCaret")
mlflow.set_tag("task_type", "classification")
mlflow.set_tag("experiment_type", "AutoML")
mlflow.set_tag("version", "1.0")

mlflow.end_run()
    
print("PyCaret AutoML experiment logged to MLflow!")