import mlflow

experiment_name = "ElasticNet"
entry_point = "Training"

mlflow.projects.run(
    uri=".",
    entry_point=entry_point,
    experiment_name=experiment_name,
    env_manager="conda"
)