FROM python:3.13-slim

WORKDIR /app

RUN pip install --no-cache-dir mlflow==3.4.0 psycopg2-binary==2.9.10 boto3

# Fix permissions for /app/artifacts directory
RUN mkdir -p /app/artifacts && chmod 755 /app/artifacts

EXPOSE 5000