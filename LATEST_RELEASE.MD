# 0.28.0-beta25 NATS JMS/MQ Bridge

#### TAG: 0.28.0-beta25

## Issues

* Make transforms configurable via the yaml file #279

_____


#### Sample Bridge config file - nats-bridge.yaml.

```yaml
---
name: "Starter Config"
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
  transforms: [ "addHeader2" ]
  replyTransforms: [ "changeBody" ]
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

transforms:
- name: "addHeader2"
  className: "io.nats.bridge.transform.examples2.AddHeaderTransform2"
  config:
    H1: H1

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
        io.nats.client.reconnect.wait: "3000"
        io.nats.client.reconnect.max: "10"
        io.nats.client.timeout: "4000"
        io.nats.client.tls.jssl.enable: "true"
        io.nats.client.tls.truststore.path: "../certs/truststore.jks"
        io.nats.client.tls.keystore.path: "../certs/keystore.jks"
        io.nats.client.tls.algorithm: "SunX509"
        io.nats.client.tls.keystore.password: "cloudurable1"
        io.nats.client.tls.truststore.password: "cloudurable2"
        #io.nats.client.tls.keystore.env_var.base64password: "NATS_BRIDGE_KEY_PASS_ENV"
        #io.nats.client.tls.truststore.env_var.base64password: "NATS_BRIDGE_TRUST_PASS_ENV"
        # NATS_BRIDGE_KEY_PASS_ENV=Y2xvdWR1cmFibGUx;NATS_BRIDGE_TRUST_PASS_ENV=Y2xvdWR1cmFibGUy
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

#### Sample configurable transform

```kotlin
package io.nats.bridge.transform.examples2;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.Configurable;
import io.nats.bridge.messages.transform.TransformResult;

import java.util.Properties;

public class AddHeaderTransform2 implements TransformMessage, Configurable  {

    private Properties properties;

    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        System.out.println("Class AddHeaderTransform: "+ builder.getHeaders());

        final Object rawValue = builder.getHeaders().get(properties.getProperty("H1"));
        final String headerValue = rawValue!= null  ? rawValue.toString() : "";
        System.out.println(headerValue);
        if (headerValue.equals("Hello")) {
            System.out.println("Headers!: "+ builder.getHeaders());
            builder.withHeader(properties.getProperty("H1"), "Hello World");
        } else {
            builder.withHeader( properties.getProperty("H1"), "Say hello please! The world awaits");
        }

        // builder.withReplyHandler(inputMessage::reply); NOTE: initFromMessage does this by default!
        return TransformResult.success("Added a new header", builder.build());
    }

    @Override
    public String name() {
        return "addHeader";
    }

    @Override
    public void configure(final Properties properties) {
        this.properties = properties;
    }
}

```
