# Use an existing docker image as a base
FROM nats:alpine3.11

# Download and install a dependency

#Copying certificates to folder /usr
COPY cert/*.* ./usr/

# Tell the image what to do when it starts as a container
#EXPOSE 4222
#EXPOSE 8222

#CMD sh -c "nats-server --tls --tlscert=./usr/localhost.pem --tlskey=./usr/localhost-key.pem --tlsverify=false"
