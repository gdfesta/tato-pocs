#!/bin/bash

# Start Ollama server in the background
ollama serve &

# Wait for the server to be ready
echo "Waiting for Ollama server to start..."
sleep 5

# Try to connect to ollama until it's ready
while ! ollama list >/dev/null 2>&1; do
    echo "Ollama server not ready yet, waiting..."
    sleep 2
done

echo "Ollama server is ready!"

# Check if deepseek-r1:1.5b model exists
if ! ollama list | grep -q "deepseek-r1:1.5b"; then
    echo "Pulling deepseek-r1:1.5b model..."
    ollama pull deepseek-r1:1.5b
    echo "Model deepseek-r1:1.5b pulled successfully!"
else
    echo "Model deepseek-r1:1.5b already exists."
fi

# Keep the container running
wait