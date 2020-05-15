#!/bin/sh
set -x

echo Starting NGINX

nginx

echo Starting NATS JMS Bridge
bin/nats-bridge-admin

#wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.3.0-Alpha1/nats-bridge-admin-boot-0.3.0-ALPHA1.zip
#unzip nats-bridge-admin-boot-0.3.0-ALPHA1.zip
