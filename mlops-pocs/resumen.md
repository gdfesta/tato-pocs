# Machine Learning Lifecycle

## Business Understanding
- Definir el problema de negocio.
- Identificar los objetivos del proyecto.
- Determinar los requisitos y restricciones.

## Data acquisition
- Recolectar datos relevantes.
- Evaluar la calidad de los datos.
- Almacenar los datos de manera segura.

## Data Processing
- Limpiar y transformar los datos.
- Seleccionar características relevantes.

Incluye cosas como:
- **Data wrangling**: Proceso de transformar y mapear datos de un formato "crudo" a otro formato que sea más adecuado para el análisis.
- **Data cleaning**: Identificación y corrección de errores o inconsistencias en los datos.
- **Data Visualization**: Representación gráfica de los datos para facilitar su comprensión.
- **Data Preprocessing**: Preparación de los datos para el modelado, incluyendo normalización y codificación.
- **Data Transformation**: Aplicación de técnicas para modificar la estructura o formato de los datos.

## Model Building
- Seleccionar algoritmos y técnicas de ML.
- Entrenar y validar modelos.
- Evaluar el rendimiento del modelo.

## Model Deployment
- Implementar el modelo en un entorno de producción.
- Integrar el modelo con aplicaciones existentes.
- Asegurar la escalabilidad y disponibilidad del modelo.

## Model Monitoring and Maintenance
- Supervisar el rendimiento del modelo en producción.
- Detectar y gestionar el deterioro del modelo.

## Model Retraining
- Actualizar el modelo con nuevos datos.
- Mejorar el rendimiento del modelo con técnicas avanzadas.
- Reentrenar el modelo periódicamente para mantener su precisión.

# Deep Dive on Productionize ML Models

![alt text](<productionize-ml-models.png>)

![alt text](<machine-learning-production-modules.png>)

# MLOps

MLOps es la práctica de combinar el desarrollo de modelos de aprendizaje automático (ML) con las operaciones de TI para automatizar y mejorar el ciclo de vida del modelo. Se basa en principios similares a DevOps, pero se adapta a las necesidades específicas del desarrollo y despliegue de modelos de ML.

Un set de practicas para estandarizar y optimizar el ciclo de vida del aprendizaje automático, desde la creación y entrenamiento de modelos hasta su despliegue y monitoreo en producción.

![alt text](<mlops.png>)

## MLFlow
MLflow es una plataforma de código abierto para gestionar el ciclo de vida del aprendizaje automático. Proporciona herramientas para rastrear experimentos, empaquetar código en reproducibles y desplegar modelos en diversos entornos.

Library agnóstica, lo que significa que puede trabajar con cualquier biblioteca de ML y cualquier lenguaje de programación.

### Componentes de MLflow

1. **MLflow Tracking**: Permite registrar y consultar experimentos de ML, incluyendo parámetros, métricas y artefactos.
2. **MLflow Projects**: Facilita la empaquetación de código de ML en un formato reproducible.
3. **MLflow Models**: Proporciona una forma estándar de empaquetar modelos para su despliegue en diversos entornos.
4. **MLflow Registry**: Un repositorio centralizado para gestionar el ciclo de vida de los modelos, incluyendo versiones y etapas de despliegue.

#### MLFlow Tracking

Allows you track and log parameters, code versions, metrics, and output files when running machine learning code.

Provide a web-based UI for visualizing and comparing different runs.

##### Experiment and Run
An **experiment** is a collection of runs for a particular project or task. It helps organize and manage related runs, making it easier to compare and analyze results.

A **Run** is a single execution of a machine learning code, which can be logged with hyperparameters, metrics, tags, etc.

##### Tracking Server

**Tracking Server**: A server that stores and manages the tracking data. It can be set up locally or on a remote server.

**Storage** backends supported include:
- File system
- SQLAlchemy-compatible databases (e.g., MySQL, PostgreSQL, SQLite)

![alt text](<tracking-server-storage.png>)

**Network** backends supported include:
- REST API
- gRPC

![alt text](<tracking-server-network.png>)

#### MLFlow Models

A standard format for packaging machine learning models to facilitate deployment across various platforms.

Saves the model in distinct flavors.

##### Storage Format
MLflow Models format is a convention for storing machine learning models. How the model is packaged and stored.

It includes:
- A directory structure
- A `MLmodel` file that describes the model and its flavors
- The actual model files

##### Model Signature
A description of the model's input and output schema, which helps ensure compatibility during deployment and inference.

It is used to generate a Rest API for the model. 

**Column-Based Schema**: Defines the input and output schema using column names and data types.

**Tensor-Based Schema**: Defines the input and output schema using tensor shapes and data types.

There are different signature enforcement:
- **Signature Enforcement**: Validates input data against the model signature during inference.
- **Name-ordering Enforcement**: Ensures that input data is provided in the correct order based on the model signature.
- **Input-type Enforcement**: Validates the data types of input data against the model signature during inference.

##### Model API
A set of standardized methods for loading and using the model, regardless of the underlying framework or library.

It can be used for real-time inference or batch scoring.

##### Model Flavors
Different ways to represent and use the model, depending on the framework or library used to create it.

##### Model Miscellaneous
It provides functionality to evaluate the model using metrics.

#### MLFlow Registry

A centralized model store for versions and metadata.

Provides APIs and a UI for managing the model lifecycle, including stages like "Staging" and "Production".

It has a search functionality to find models based on various criteria. Including metadata like creation date, version, and tags.

![alt text](<model-registry.png>)

#### MLFlow Projects

Simplifies the process of sharing and reproducing machine learning code.

A project is a directory with code and a descriptor file (MLproject) that specifies its dependencies and entry points.

MLproject file is a YAML file that defines the project structure, dependencies, and entry points.

It can specify:
- The project name
- The conda environment
- The entry points (commands to run the project)

It supports different execution environments, including local, Docker, and cloud platforms.

### MLFlow in AWS

AWS provides managed services that can be integrated with MLflow for a seamless machine learning workflow.

These services include:
- Amazon SageMaker: A fully managed service for building, training, and deploying machine learning models.
- Amazon S3: Scalable storage for data and model artifacts.
- Amazon RDS: Managed relational database service for storing metadata and experiment tracking.

![alt text](<aws-basic-flow-diagram.png>)

See [readme.md](./udemy-course/aws_example/README.md) for more details on the AWS example.