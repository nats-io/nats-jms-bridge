# Build admin image 
docker build -t cloudurable/nats-bridge-admin -f cicd/Dockerfile_bridge.dev .

# Publish admin image
docker push cloudurable/nats-bridge-admin
