
> direnv allow


there is also a docker_env option

> mlflow run . -P alpha=0.5 -P l1_ratio=0.5 --experiment-name=experiment_1 --entry-point ElasticNet

> conda env export --name mlflow_poc_py311 > conda.yaml