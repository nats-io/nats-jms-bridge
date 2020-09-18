#!/bin/sh
set -e

VERSION=${VERSION:-0.22.1-beta19}
DOCKER_NAMESPACE=${DOCKER_NAMESPACE:-synadia}

wrapper() {
  cd message
  gradle wrapper
  pwd
  cd ..
  cd core
  gradle wrapper
  pwd
  cd ..
  cd admin
  gradle wrapper
  pwd
  cd ..
}


build_all() {
  cd message
  ./gradlew clean build publishToMavenLocal -x test
  pwd
  cd ..
  cd core
  ./gradlew clean build publishToMavenLocal -x test
  pwd
  cd ..
  cd admin
  ./gradlew clean distZip
  pwd
  cd ..
}

test_all() {
  cd message
  ./gradlew clean build
  pwd
  cd ..
  cd core
  ./gradlew clean build
  pwd
  cd ..
  cd admin
  ./gradlew clean build
  pwd
  cd ..
}

build_prometheus_image() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/prometheus prometheus
}

build_bridge_nats_server_image() {
  bin/build_deploy_docker.sh "$VERSION" "$DOCKER_NAMESPACE" cicd/bridge-nats-server-tls bridge-nats-server
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

build_admin_image_local() {
  build_bridge
  cp admin/build/distributions/nats-bridge-admin-0*.zip cicd/nats-bridge-admin-local/dist.zip
  bin/build_deploy_docker.sh "$VERSION-b2" "$DOCKER_NAMESPACE" cicd/nats-bridge-admin-local nats-bridge-admin
}


build_bridge() {
  cd core
  ./gradlew clean build publishToMavenLocal -x test
  cd ..
  cd admin
  ./gradlew clean distZip distTar bootJar bootDistZip bootDistTar
  cd ..
}

build_install_dir() {
  build_bridge
  cd admin
  ./gradlew installDist
  cd ..
}

prepare_ibm_mq_test() {
  build_install_dir
  mkdir admin/build/install/nats-bridge-admin/config
  cp  admin/build/install/nats-bridge-admin/nats-bridge-ibm-mq-demo-conf.yaml admin/build/install/nats-bridge-admin/config/nats-bridge.yaml
  echo "Now run admin/build/install/nats-bridge-admin/bin/nats-bridge-admin and integration.sh"
}

prepare_ibm_mq_env_test() {
  build_install_dir
  mkdir admin/build/install/nats-bridge-admin/config
  cp  admin/build/install/nats-bridge-admin/nats-bridge-ibm-mq-no-conf.yaml admin/build/install/nats-bridge-admin/config/nats-bridge.yaml
  echo "Now run admin/build/install/nats-bridge-admin/bin/nats-bridge-admin and integration.sh"
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
  docker ps  | grep "cicd_" | awk '{print $1}' | xargs docker stop
  docker ps -a | grep "cicd_" | awk '{print $1}' | xargs docker rm
  docker images | grep "cicd_" | awk '{print $3}' | xargs docker  rmi

}

copy_ibm_test_config() {
  echo "copying sample conf nats-bridge-ibm-mq-demo-conf.yaml to admin conf"
  touch  admin/config/nats-bridge.bak
  echo "-----back up------" >> admin/config/nats-bridge.bak
  cat admin/config/nats-bridge.yaml >> admin/config/nats-bridge.bak
  cp admin/sampleConf/nats-bridge-ibm-mq-demo-conf.yaml admin/config/nats-bridge.yaml
}

docker_deploy_ibm_only() {

  cd cicd
  docker-compose  -f compose/docker-compose-ibm-only.yml up
  cd ..
}

docker_deploy_nats_only() {

  cd cicd
  docker-compose  -f compose/docker-compose-nats-only.yml up
  cd ..
}


docker_deploy_ibm_mq_test() {
  copy_ibm_test_config
  cd cicd
  docker-compose  -f compose/docker-compose-ibm-test.yml build
  docker-compose  -f compose/docker-compose-ibm-test.yml up
  cd ..
}

copy_nats_tls_test_config() {
  echo "copying sample conf admin/sampleConf/nats-bridge-nats-tls.yaml to admin conf"
  touch  admin/config/nats-bridge.bak
  echo "-----back up------" >> admin/config/nats-bridge.bak
  cat admin/config/nats-bridge.yaml >> admin/config/nats-bridge.bak
  cp admin/sampleConf/nats-bridge-nats-tls.yaml admin/config/nats-bridge.yaml
}

docker_deploy_nats_tls_test() {
  copy_nats_tls_test_config
  cd cicd
  docker-compose  -f compose/docker-compose-nats-tls.yml build
  docker-compose  -f compose/docker-compose-nats-tls.yml up
  cd ..
}

docker_build_nats_tls_test() {
  copy_nats_tls_test_config
  cd cicd
  docker-compose  -f compose/docker-compose-nats-tls.yml build
  cd ..
}

help () {
  echo "Valid commands:"
  echo "Docker Builds:"
  echo "Use 'build_ibm_mq_image', bimi to build IBM image"
  echo "Use 'build_admin_image', bai, admin to build NATs bridge admin"
  echo "Use 'clean_docker_images' | clean_docker | ci to clear out docker images"
  echo "Use 'build_prometheus_image' | bpi to build prometheus which can scrape admin"
  echo "Use 'build_bridge_nats_server_image' | nats to build prometheus which can scrape admin"
  echo "Use 'build_bridge_activemq' | activemq to build activemq"
  echo "Use 'build_gradle_image' to build travis image for testing"
  echo "Use 'build_travis_build_image' to build travis image for testing"
  echo "Docker Compose:"
  echo "Use 'localdev' to run all images for local development"
  echo "Use 'docker_deploy_ibm_mq_test' to run a version of IBM MQ that has non default values use config sample nats-bridge-ibm-mq-demo-conf.yaml"
  echo "Use 'multbridge' to run all images and 3 bridges for testing work share"
  echo "Use 'multibm' to run a version with 3 IBM MQ to test failover "
  echo "Gradle Builds Compose:"
  echo "Use build_install_dir to create install dir"
  echo "Use build_admin_image_local or bai_local to build a admin image that does not depend on a release"
  echo "QA integration tests:"
  echo "Use 'prepare_ibm_mq_test' to prepare for IBM MQ example config in yaml"
  echo "Use 'prepare_ibm_mq_env_test' to prepare for IBM MQ config with env vars only"
  echo "Use 'docker_deploy_ibm_mq_test' used to test not using any IBM MQ defaults"
  echo "Use 'docker_deploy_nats_tls_test' used to test using opentls with NATS java"

}



export COMMAND="$1"

case $COMMAND in

test_all)
  test_all
  ;;

wrapper)
    wrapper
    ;;

build_all)
  build_all
  ;;


docker_deploy_nats_only)
  docker_deploy_nats_only
  ;;

docker_deploy_ibm_only)
    docker_deploy_ibm_only
    ;;

docker_build_nats_tls_test)
  docker_build_nats_tls_test
  ;;

docker_deploy_nats_tls_test)
  docker_deploy_nats_tls_test
  echo "Done!"
  ;;

prepare_ibm_mq_env_test)
  prepare_ibm_mq_env_test
  echo "Done!"
  ;;

prepare_ibm_mq_test)
  prepare_ibm_mq_test
  echo "Done!"
  ;;

build_install_dir)
  build_install_dir
  echo "Done!"
  ;;

build_admin_image_local | bai_local)
  build_admin_image_local
  echo "Work complete!"
  ;;

build_bridge | bb)
  build_bridge
  echo "Work complete!"
  ;;

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

stop-localdev)
  cd cicd
  docker-compose  stop
  docker-compose  rm
  cd ..
  ;;

  multbridge)
          bin/docker-deploy-multbridge.sh
          ;;

  stop-multbridge)
    cd cicd
    docker-compose  -f compose/docker-compose-nats-tls-multbridge stop
    docker-compose  -f compose/docker-compose-nats-tls-multbridge rm
    cd ..
    ;;

    multibm)
            bin/docker-deploy-multibm.sh
            ;;

    stop-multibm)
      cd cicd
      docker-compose  -f compose/docker-compose-multibm.yaml stop
      docker-compose  -f compose/docker-compose-multibm.yaml rm
      cd ..
      ;;

stop-localdev-nats-tls)
    cd cicd
    docker-compose  -f compose/docker-compose-nats-tls.yml stop
    docker-compose  -f compose/docker-compose-nats-tls.yml rm
    cd ..
    ;;


docker_deploy_ibm_mq_test)
      docker_deploy_ibm_mq_test
      echo "Done!"
      ;;

help)
  help
  ;;

*)
  help
  ;;

esac
