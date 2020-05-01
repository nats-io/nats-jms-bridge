# Build admin image
docker build -t cloudurable/gradle -f cicd/Dockerfile_gradle.dev .

# Publish admin image
docker push cloudurable/gradle

docker tag cloudurable/gradle cloudurable/gradle:0.0.1
docker push cloudurable/gradle:0.0.1
