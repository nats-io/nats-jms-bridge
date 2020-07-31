FROM ubuntu:20.04

#Copying files
COPY . ./usr/app

WORKDIR /usr/app

#Install some dependencies
RUN apt-get update
RUN apt-get -y install curl unzip zip

# Install SDK MAN
RUN chmod +x cicd/sdkman.sh
RUN exec cicd/sdkman.sh

# Install Java
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install java 8.0.262-amzn"
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install gradle"


ENV JAVA_HOME /root/.sdkman/candidates/java/current

#Build Message
WORKDIR /usr/app/message
RUN bash -c "./gradlew wrapper"
RUN bash -c "./gradlew clean build publishToMavenLocal -x test"

#Build Core
WORKDIR /usr/app/core
RUN bash -c "./gradlew wrapper"
RUN bash -c "./gradlew clean build publishToMavenLocal -x test"

#Build Admin
WORKDIR /usr/app/admin
RUN bash -c "./gradlew wrapper"
RUN bash -c "./gradlew build -x test"

WORKDIR /usr/app
