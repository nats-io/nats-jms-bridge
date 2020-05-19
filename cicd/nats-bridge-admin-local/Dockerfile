## Using Nginx with a base image
FROM nginx:1.17.10


## Some environment variables
# Bridge Version


## Install some dependencies
# Update the packages
RUN apt-get update
# Installing wget(Bridge app, Mkcert and Nginx), curl, zip and unzip (sdkman), bash (run scripts), rc (run services linux)
RUN apt-get -y install wget curl zip unzip
#Installing Sdkman
COPY sdkman.sh /opt/temp/sdkman.sh
COPY install.sh /opt/temp/install.sh
COPY dist.zip /opt/bridge/dist.zip

# Install SDK MAN
RUN chmod +x /opt/temp/*.sh

RUN  /opt/temp/sdkman.sh


RUN /opt/temp/install.sh

# Java Home
ENV JAVA_HOME /root/.sdkman/candidates/java/current

## Configuring Nginx
# Copy config file
COPY nginx/admin.conf /etc/nginx/conf.d/admin.conf
# Copy certificate
COPY cert/localhost.pem /usr/app/certs/localhost.pem
# Copy certificate key
COPY cert/localhost-key.pem /usr/app/certs/localhost-key.pem
# Start nginx
#RUN bash -c "service nginx restart"


# Enter bridge folder
WORKDIR /opt/bridge


# Unpack version
RUN unzip dist.zip

RUN ls

# Delete the zip file
RUN rm *.zip

RUN mv nats-bridge-admin* nats-bridge-admin

# Entering in the bridge folder
WORKDIR /opt/bridge/nats-bridge-admin


# Create a config folder
RUN mkdir config

# Move the nat-bridge yaml to config folder
RUN mv nats-bridge.yaml config/


## Run the server
#Copy start file
COPY nginx/start_prod.sh /opt/bridge/nats-bridge-admin/start_prod.sh
RUN chmod 777 start_prod.sh

# Start services
CMD bash -c "./start_prod.sh"
