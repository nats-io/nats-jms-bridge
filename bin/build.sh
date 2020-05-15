#!/bin/sh
set -e

VERSION=${VERSION:-0.4.0-beta1}
DOCKER_NAMESPACE=${DOCKER_NAMESPACE:-synadia}

build_prometheus_image() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/prometheus prometheus
}

build_bridge_nats_server_image() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/bridge-nats-server bridge-nats-server
}

build_bridge_activemq() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/bridge-activemq bridge-activemq
}

build_ibm_mq_image() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/bridge-ibmmq bridge-ibmmq
}

build_admin_image() {
  bin/build_deploy_docker.sh
}

build_gradle_image() {

  DOCKER_NAME="bridge-gradle"

  echo "Build docker image $DOCKER_NAMESPACE/$DOCKER_NAME"
  docker build -t $DOCKER_NAMESPACE/$DOCKER_NAME -f cicd/gradle/Dockerfile .

  echo "Publish docker image "
  docker push $DOCKER_NAMESPACE/$DOCKER_NAME


  echo "Version and republish docker image $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION"
  docker tag $DOCKER_NAMESPACE/$DOCKER_NAME $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION
  docker push $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION

}

build_travis_build_image() {

  DOCKER_NAME="bridge-travis-build"

  echo "Build docker image $DOCKER_NAMESPACE/$DOCKER_NAME"
  docker build -t $DOCKER_NAMESPACE/$DOCKER_NAME -f cicd/build/Dockerfile .

  echo "Publish docker image "
  docker push $DOCKER_NAMESPACE/$DOCKER_NAME

  echo "Version and republish docker image $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION"
  docker tag $DOCKER_NAMESPACE/$DOCKER_NAME $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION
  docker push $DOCKER_NAMESPACE/$DOCKER_NAME:$VERSION

}

clean_docker_images() {
  docker ps  | grep $DOCKER_NAMESPACE | awk '{print $1}' | xargs docker stop
  docker ps -a | grep $DOCKER_NAMESPACE | awk '{print $1}' | xargs docker rm
  docker images | grep $DOCKER_NAMESPACE | awk '{print $3}' | xargs docker  rmi
}

help () {
  echo "Valid commands:"
  echo "Use build_ibm_mq_image, bimi to build IBM image"
  echo "Use build_admin_image, bai, admin to build NATs bridge admin"
  echo "Use clean_docker_images | clean_docker | ci to clear out docker images"
  echo "Use build_prometheus_image | bpi to build prometheus which can scrape admin"
  echo "Use build_bridge_nats_server_image | nats to build prometheus which can scrape admin"
  echo "Use build_bridge_activemq | activemq to build activemq"
  echo "Use build_gradle_image to build travis image for testing"
  echo "Use build_travis_build_image to build travis image for testing"
  echo "Use localdev to run all images for local development"
}

export COMMAND="$1"

case $COMMAND in

build_ibm_mq_image |  bimi)
  build_ibm_mq_image
  echo "Work complete!"
  ;;

build_admin_image | bai | admin)
  build_admin_image
  echo "Work complete!"
  ;;

clean_docker_images | clean_docker | ci)
  clean_docker_images
  echo "Work complete!"
  ;;

build_prometheus_image | bpi )
  build_prometheus_image
  echo "Work complete!"
  ;;

build_bridge_nats_server_image | nats)
      build_bridge_nats_server_image
      echo "Work complete!"
      ;;


build_bridge_activemq | activemq)
    build_bridge_activemq
    echo "Work complete!"
    ;;

build_gradle_image)
    build_gradle_image
    echo "Work complete!"
    ;;

build_travis_build_image)
      build_travis_build_image
      echo "Done!"
      ;;

localdev)
        bin/docker-deploy-local-dev.sh
        ;;

help)
  help
  ;;

*)
  help
  ;;

esac
