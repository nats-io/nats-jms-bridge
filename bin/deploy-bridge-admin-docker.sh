# Build admin image
docker build -t cloudurable/nats-bridge-admin -f cicd/Dockerfile_bridge.dev .

# Publish admin image
docker push cloudurable/nats-bridge-admin


docker tag cloudurable/nats-bridge-admin cloudurable/nats-bridge-admin:0.0.1
docker push cloudurable/nats-bridge-admin:0.0.1
