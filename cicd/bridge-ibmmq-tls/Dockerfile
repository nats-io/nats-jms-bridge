# Use an existing docker image as a base
FROM ibmcom/mq:9.1.5.0-r1

#Copy file and change the owner
#IBM MQ can read 11-dev that creates a queue model.
COPY --chown=1001:root 11-dev.mqsc /etc/mqm/11-dev.mqsc
COPY --chown=1001:root ibmmqcert/key.crt /etc/mqm/pki/keys/key/key.crt
COPY --chown=1001:root ibmmqcert/key.key /etc/mqm/pki/keys/key/key.key

# environment
ENV LICENSE=accept

# Queue manager
ENV MQ_QMGR_NAME=QM1

# Tell the image what to do when it starts as a container
EXPOSE 1414


#ENV MQ_APP_PASSWORD=/your password here/
