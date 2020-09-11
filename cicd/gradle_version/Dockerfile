FROM ubuntu:20.04

#Copying files
COPY . ./usr/app

WORKDIR /usr/app

#Install some dependencies
RUN apt-get update
RUN apt-get -y install curl unzip zip wget nano jq

# Install SDK MAN
RUN chmod +x cicd/sdkman.sh
RUN exec cicd/sdkman.sh

# Install Java
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install java 8.0.265-amzn"
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install gradle"


ENV JAVA_HOME /root/.sdkman/candidates/java/current

#Downloading version
WORKDIR /usr/app/bridge
RUN bash -c "wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.21.2-beta18/nats-bridge-admin-0.21.2-beta18.zip"
RUN bash -c "unzip nats-bridge-admin-0.21.2-beta18.zip"
RUN bash -c "rm *.zip"

RUN bash -c "mkdir certs"

RUN bash -c "cp -r /usr/app/certs/* certs/"
