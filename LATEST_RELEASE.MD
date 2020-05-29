
# NATS Bridge Beta2


Added config changes to demonstrate using different IBM MQ servers and passing NATs client config via the admin YAML config.

* #101 Full integration test used to verify Queue to Queue forward bridge
* #59 Verifying that Queue to Queue forward integration test works with the admin (most of our full integration tests were done with REQUEST/REPLY but Queue to Queue forward testing is in core).
* #118 FORWARD Bridge does not need a QModel question  
* #119 FORWARD Bridge does not need a QModel fix
* #99 Bug with builder config that we noticed while writing #101
* #100 Fixed `integration.sh` which broke between alpha1 and beta1 (for testing install)
* #103 Created docker container with different setting for the channel, queue model, etc. to show how you can set IBM MQ params from config YAML of admin
* #102 created demo IBM MQ config to show how params can be set for a different IBM MQ server
* #97 created a config that does not use localhost for integration testing with docker-compose and for docker container for NATS bridge admin
* #98 Show NATS client config
* #90 openssl example work
* #107 test on linux using Java 8
* #114 Add auto-config for IBM MQ and Active MQ  
* #96 Example using IBM env vars from bridge admin
* #78 tls example work admin bridge
* #108 tls example docker, docker compose, test.
* #121 Nats to NATS doc changes
* #115 Session creation issue 

![Admin Console Swagger Open API](https://user-images.githubusercontent.com/382678/82646981-5598d580-9bca-11ea-9bc8-5dfa6875c61e.png)

#### FORWARD Bridge no longer needs a QModel set up (only request/reply)

You can now run FORWARD bridge w/o a QModel when using IBM MQ.

#### nats-bridge.yaml
```yaml
...
  - name: "natsForwardToJMS"
    bridgeType: "FORWARD"
    source:
      name: "natsSrc"
      busType: "NATS"
      subject: "forward-subject"
      clusterName: "natsCluster"
    destination:
      name: "ibmMQDst"
      busType: "JMS"
      subject: "DEV.QUEUE.2"
      clusterName: "ibmMqCluster"
    copyHeaders: false
    workers: 3
    tasks : 1
  - name: "natsToIBMMq"
    bridgeType: "REQUEST_REPLY"
    source:
      name: "nats"
      busType: "NATS"
      subject: "b-subject"
      clusterName: "natsCluster"
    destination:
      name: "ibmMQ"
      busType: "JMS"
      subject: "DEV.QUEUE.1"
      clusterName: "ibmMqCluster"
    copyHeaders: false
    workers: 4
    tasks : 1

clusters:
  jmsCluster:
    name: "jmsCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://active-mq:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      userName: "cloudurable"
      password: "cloudurable"
      jmsDestinationType: "QUEUE"
  natsCluster:
    name: "natsCluster"
    properties: !<nats>
      host: "nats-server"
      port: 4222
      servers: []
      config:
        io.nats.client.reconnect.wait: 3000
        io.nats.client.reconnect.max: 10
        io.nats.client.timeout: 4000
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
        nats.ibm.mq.host: "tcp://ibm-mq:1414"
        nats.ibm.mq.channel: "DEV.APP.SVRCONN"
        nats.ibm.mq.queueManager: "QM1"
      userName: "app"
      password: "passw0rd"
      jmsDestinationType: "QUEUE"
      autoConfig: "IBM_MQ"

```

#### Open TLS example (server and client/NATS bridge config)

Set `io.nats.client.opentls: true` in the cluster config for NATS in the NATS Bridge Admin tool as follows:

#### Sample NATS with TLS support
#### nats-bridge.yaml
```yaml
...
clusters:
   ...
  natsCluster:
    name: "natsCluster"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config:
        io.nats.client.reconnect.wait: 3000
        io.nats.client.reconnect.max: 10
        io.nats.client.timeout: 4000
        io.nats.client.opentls: true
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
        nats.ibm.mq.host: "tcp://localhost:1414"
        nats.ibm.mq.channel: "DEV.APP.SVRCONN"
        nats.ibm.mq.queueManager: "QM1"
        nats.ibm.mq.queueModelName: "DEV.MODEL"
        nats.ibm.mq.queueModelPrefix: "DEV*"
      userName: "app"
      password: "passw0rd"
      jmsDestinationType: "QUEUE"

```

#### Added ability to use env vars from bridge admin
#### sample-confg/nats-bridge-ibm-mq-no-conf.yaml
```yaml
bridges:
- name: "natsToIBMMq"
  bridgeType: "REQUEST_REPLY"
  source:
    name: "nats"
    busType: "NATS"
    subject: "b-subject"
    clusterName: "natsCluster"
  destination:
    name: "ibmMQ"
    busType: "JMS"
    subject: "MY.QUEUE.1"
    clusterName: "ibmMqCluster"
  copyHeaders: false
  workers: 5
  tasks : 2

clusters:
  jmsCluster:
    name: "jmsCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://localhost:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      userName: "cloudurable"
      password: "cloudurable"
      jmsDestinationType: "QUEUE"
  natsCluster:
    name: "natsCluster"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config:
        io.nats.client.reconnect.wait: 3000
        io.nats.client.reconnect.max: 10
        io.nats.client.timeout: 4000
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        {}
      userName: "app"
      password: "passw0rd2"
      jmsDestinationType: "QUEUE"
      autoConfig: "IBM_MQ"
```

To use this test run with ` bin/build.sh docker_deploy_test` from the root of the project which will launch docker deploy for the IBM docker image that is different than the defaults.

```
 $ pwd
/Users/richardhightower/synadia/nats-bridge
(⎈ |N/A:N/A)richardhightower@Richards-MacBook-Pro nats-bridge %
 $ bin/build.sh help
Valid commands:
Docker Builds:
Use 'build_ibm_mq_image', bimi to build IBM image
Use 'build_admin_image', bai, admin to build NATs bridge admin
Use 'clean_docker_images' | clean_docker | ci to clear out docker images
Use 'build_prometheus_image' | bpi to build prometheus which can scrape admin
Use 'build_bridge_nats_server_image' | nats to build prometheus which can scrape admin
Use 'build_bridge_activemq' | activemq to build activemq
Use 'build_gradle_image' to build travis image for testing
Use 'build_travis_build_image' to build travis image for testing
Docker Compose:
Use 'localdev' to run all images for local development
Use 'docker_deploy_test' to run a version of IBM MQ that has non default values use config sample nats-bridge-ibm-mq-demo-conf.yaml
Gradle Builds Compose:
Use build_install_dir to create install dir
Use build_admin_image_local or bai_local to build a admin image that does not depend on a release
QA integration tests:
Use 'prepare_ibm_mq_test' to prepare for IBM MQ example config in yaml
Use 'prepare_ibm_mq_env_test' to prepare for IBM MQ config with env vars only
```

#### To pass IBM MQ specific properties you can specify ENV vars or configure the cluster config
#### nats-bridge.yml
```yaml
...
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
        nats.ibm.mq.host: "tcp://ibm-mq:1414"
        nats.ibm.mq.channel: "DEV.APP.SVRCONN"
        nats.ibm.mq.queueManager: "QM1"
        nats.ibm.mq.queueModelName: "DEV.MODEL"
        nats.ibm.mq.queueModelPrefix: "DEV*"
      userName: "app"
      password: "passw0rd"
      jmsDestinationType: "QUEUE"
```

#### Setting up NATS client config
```yaml
  natsCluster:
    name: "natsCluster"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config:
        io.nats.client.reconnect.wait: 3000
        io.nats.client.reconnect.max: 10
        io.nats.client.timeout: 4000
```
All config parameters get passed to Options properties. Find the keys for Options properties [here](https://javadoc.io/doc/io.nats/jnats/latest/io/nats/client/Options.html ).

#### Example setting up env variables
```sh
# NATS_BRIDGE_IBM_QUEUE_MODEL_NAME - the name of the Queue Model used for request/replies (defaults to DEV.MODEL).
export NATS_BRIDGE_IBM_QUEUE_MODEL_NAME=MY.MODEL

#NATS_BRIDGE_IBM_QUEUE_MODEL_PREFIX - the name of the queue model prefix (defaults to DEV*).
export NATS_BRIDGE_IBM_QUEUE_MODEL_PREFIX=MY*

# NATS_BRIDGE_IBM_MQ_HOST - the host name of the IBM MQ server (defaults to value tcp://localhost:1414).
export NATS_BRIDGE_IBM_MQ_HOST=tcp://localhost:2414

# NATS_BRIDGE_IBM_MQ_CHANNEL - the channel you are using for your IBM MQ cluster (defaults to "DEV.APP.SVRCONN)
export NATS_BRIDGE_IBM_MQ_CHANNEL=MY.APP.SVRCONN

# NATS_BRIDGE_IBM_MQ_QUEUE_MANAGER - the name of the queue manager for your IBM MQ cluster (defaults to QM1).
export NATS_BRIDGE_IBM_MQ_QUEUE_MANAGER=MYQM1


```

## Docker rework.
Getting docker container to work inside of docker-compose or local.


#### Sample config that will allow admin to work inside or outside of docker-compose
#### nats-bridge-dev.yaml
```yaml

---
...
bridges:
- name: "natsToIBMMq"
  bridgeType: "REQUEST_REPLY"
  source:
    name: "nats"
    busType: "NATS"
    subject: "b-subject"
    clusterName: "natsCluster"
  destination:
    name: "ibmMQ"
    busType: "JMS"
    subject: "DEV.QUEUE.1"
    clusterName: "ibmMqCluster"
  copyHeaders: false
  workers: 5
  tasks : 2

clusters:
  jmsCluster:
    name: "jmsCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://active-mq:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      userName: "cloudurable"
      password: "cloudurable"
      jmsDestinationType: "QUEUE"
  natsCluster:
    name: "natsCluster"
    properties: !<nats>
      host: "nats-server"
      port: 4222
      servers: []
      config: {}
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
        nats.ibm.mq.host: "tcp://ibm-mq:1414"
        nats.ibm.mq.channel: "DEV.APP.SVRCONN"
        nats.ibm.mq.queueManager: "QM1"
        nats.ibm.mq.queueModelName: "DEV.MODEL"
        nats.ibm.mq.queueModelPrefix: "DEV*"
      userName: "app"
      password: "passw0rd"
      jmsDestinationType: "QUEUE"

```

#### Dev Names for NATS Bridge
#### /etc/hosts --- on developers box
```
## Dev names for NATS Bridge
127.0.0.1	nats-server
127.0.0.1	ibm-mq
127.0.0.1	active-mq
127.0.0.1 bridge-admin
```

The dev file will now be included in the zip as a sample config.
