# 0.15.0-beta10 NATS JMS/MQ Bridge

#### TAG: 0.15.0-beta10

## Issues

* #190 Support truststore in .crt format.
* #191 Adding support for transform on forward.  
* #192 Adding support for transform on reply.

----

## #191 and #192 Example Forward Transform

#### nats-jms-mq-bridge/example-transform/config/nats-bridge-forward-q-2-q.yaml

```yaml

---
name: "Starter Config"
...
bridges:
  - name: "natsForwardToJMS"
    bridgeType: "FORWARD"
    transforms: ["addHeader"] # <----------------- Look here <<<<<<<<<<<<<<
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
...
```


## Example Request/Reply Transform

#### nats-jms-mq-bridge/example-transform/config/nats-bridge-reply-transform.yaml

```yaml
---
name: "Config for reply example"
dateTime:
  - 2020
  - 4
  - 30
  - 0
  - 53
  - 50
  - 423615000
bridges:
  - name: "natsToIBMMq"
    bridgeType: "REQUEST_REPLY"
    transforms: ["addHeader"]
    replyTransforms : ["changeBody"]
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
    ...

```

## #190 Ability to read crt files as well as jks

```java
SslContextBuilder sslContext = new SslContextBuilder();
sslContext.withAlgorithm("SunX509");
sslContext.withKeystorePath("../certs/keystore.crt");
sslContext.withTruststorePath("../certs/truststore.jks");
sslContext.withKeyPassword("cloudurable1");
sslContext.withStorePassword("cloudurable2");


NatsMessageBusBuilder natsMessageBusBuilder = new NatsMessageBusBuilder();
natsMessageBusBuilder.withName("natsCluster");
natsMessageBusBuilder.withUseTls(true);
natsMessageBusBuilder.withSubject("a1-subject");
natsMessageBusBuilder.withQueueGroup("exampleGroup");
natsMessageBusBuilder.withSslContext(sslContext.build());
natsMessageBusBuilder.getOptionsBuilder().server("nats://localhost:4222");
natsMessageBusBuilder.getOptionsBuilder().noReconnect();

MessageBus messageBus = natsMessageBusBuilder.build();

System.out.println("Connect using CRT Cert - Good bye");
messageBus.close();
```
