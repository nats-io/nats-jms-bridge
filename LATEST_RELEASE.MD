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
    transforms: ["addHeader"] # <----------------- Look here <<<<<<<<<<<<<<
    replyTransforms : ["changeBody"] # <----------------- And Look here <<<<<<<<<<<<<<
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

## Added new change body transform example

#### nats-jms-mq-bridge/example-transform/src/main/java/io/nats/bridge/transform/examples/ModifyBodyTransform.java

```java
package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class ModifyBodyTransform implements TransformMessage {


    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        System.out.println("Class ModifyBodyTransform: "+ inputMessage.bodyAsString());

        builder.withBody(inputMessage.bodyAsString() + " FROM BRAZIL!");

        return TransformResult.success("Message came from Brazil", builder.build());
    }

    @Override
    public String name() {
        return "changeBody";
    }
}


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
