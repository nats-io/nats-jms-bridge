#!/bin/sh
set -e -i

VERSION=${1:-0.23.0-beta20-INTERIM}
DOCKER_NAMESPACE=${2:-synadia}
DOCKER_DIR=${3:-cicd/nats-bridge-admin/}
DOCKER_NAME=${4:-nats-bridge-admin}


CWD=$(pwd)
cd "$DOCKER_DIR"

pwd



echo "Build docker image $DOCKER_NAMESPACE/$DOCKER_NAME"
docker build -t $DOCKER_NAMESPACE/$DOCKER_NAME  .

echo "Publish docker image "
docker push $DOCKER_NAMESPACE/$DOCKER_NAME


echo "Version and republish docker image $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION"
docker tag $DOCKER_NAMESPACE/$DOCKER_NAME $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION
docker push $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION


cd "$CWD"

pwd
