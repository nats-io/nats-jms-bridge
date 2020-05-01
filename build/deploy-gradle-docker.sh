# Build admin image
docker build -t cloudurable/gradle -f cicd/Dockerfile_gradle.dev .

# Publish admin image
docker push cloudurable/gradle
