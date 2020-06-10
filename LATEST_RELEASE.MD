# 0.9.0-beta5 NATS JMS/MQ Bridge

#### TAG: 0.9.0-beta5

## Issues

* #136 Env vars and config files readable from filesystem and classpath
* #146 JSSL example config showing how to use truststore, keystore, etc. from admin and from examples
* #135 Passing CCDT file with a client requires URL that is HTTP, not FILE, this does not work, we have equiv functionality for the bridge w/o using this CCDT file. It is redundant and does not work.

___

## Using ENV variables and command line args to use classpath://
```
Usage: run [OPTIONS]

  Run NATS JMS/IBM MQ Bridge

Options:
  -d, --config-directory TEXT    Location of Configuration Directory
  -f, --bridge-config-file TEXT  Location of Bridge Config File
  -l, --login-config-file TEXT   Location of Bridge Login Config File
  -h, --help                     Show this message and exit

You can also set environments variables by replacing dashes '-' with
underscores '_' and prefix with "NATS_BRIDGE"

NATS_BRIDGE_LOGIN_CONFIG_FILE=./config/nats-bridge-logins.yaml
NATS_BRIDGE_BRIDGE_CONFIG_FILE=./config/nats-bridge.yaml

Files can also be on the classpath inside of a jar file or on the file system
in the classpath. Just prepend the file name with "classpath://" to denote
looking for this file on the classpath instead of the file system.

-f classpath://nats-bridge.yaml
```

____

## Using JSSL with NATS Bridge and NATS simple example

A few examples were added to show the use of JSSL with NATS Bridge.

`$project_home/nats-jms-mq-bridge/examples/src/main/java/io/nats/bridge/examples/jms/tls`

#### nats-bridge.yaml bridge config file showing JSSL setup for NATS.

```yaml
---
name: "IBM MQ to NATS Request/Reply Example No QModel"
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
    source:
      name: "nats"
      busType: "NATS"
      subject: "request_reply_jms_tls"
      clusterName: "natsCluster"
    destination:
      name: "ibmMQ"
      busType: "JMS"
      subject: "DEV.QUEUE.1"
      responseSubject: "DEV.QUEUE.2"
      clusterName: "ibmMqCluster"

    copyHeaders: false
    workers: 1
    tasks : 1

clusters:
  ibmMqCluster:
    name: "ibmMqCluster"
    properties: !<jms>
      config:
        java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
        nats.ibm.mq.host: "tcp://localhost:1414"
        nats.ibm.mq.channel: "DEV.APP.SVRCONN"
        nats.ibm.mq.queueManager: "QM1"
      userName: "app"
      password: "passw0rd"
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
        io.nats.client.tls.jssl.enable: true
        io.nats.client.tls.truststore.path: ../certs/truststore.jks
        io.nats.client.tls.keystore.path: ../certs/keystore.jks
        io.nats.client.tls.algorithm: SunX509
        io.nats.client.tls.store.password: cloudurable
        io.nats.client.tls.trust.password: cloudurable

```

#### SimpleNatsClientSsl.java

```java
package io.nats.bridge.examples.jms.tls.ibmmq;

import io.nats.bridge.examples.ssl.SslContextBuilder;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleNatsClientSsl {
    public static void main(String[] args) {
        try {

            SslContextBuilder sslContextBuilder = new SslContextBuilder();
            sslContextBuilder.withTruststorePath("../certs/truststore.jks");
            sslContextBuilder.withKeystorePath("../certs/keystore.jks");
            sslContextBuilder.withKeyPassword("cloudurable");
            sslContextBuilder.withStorePassword("cloudurable");
            var sslContext = sslContextBuilder.build();

            final Options.Builder builder = new Options.Builder().sslContext(sslContext)
                    .server("nats://localhost:4222");

            final Connection connect = Nats.connect(builder.build());

            final Message replyFromJmsServer = connect.request("request_reply_jms_tls",
                    "Hello World!".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(20));

            if (replyFromJmsServer != null) {
                System.out.println("RESPONSE FROM SERVER " + new String(replyFromJmsServer.getData(), StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message sent from JMS server");
            }


        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

```

#### SimpleMqServerNoQModel.java

```java
package io.nats.bridge.examples.jms.tls.ibmmq;

import io.nats.bridge.examples.JmsBuildUtils;
import io.nats.bridge.examples.ibmmq.IbmMqUtils;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleMqServerNoQModel {
    public static void main(final String[] args) {

        final Duration waitForMessage = Duration.ofSeconds(20);

        try {
            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils()
                    .withConnectionFactory(IbmMqUtils.createJmsConnectionFactoryWithNoQModel());


            final Session session = jmsBuildUtils.getSession();


            jmsBuildUtils.withDestination(session.createQueue("DEV.QUEUE.1"));


            final MessageConsumer messageConsumer = jmsBuildUtils.getConsumerSupplier().get();

            int count = 0;

            while (count < 100) {
                System.out.println("About to get Message");
                final Message messageFromClient = messageConsumer.receive(waitForMessage.toMillis());
                System.out.println("Attempted to get Message");

                if (messageFromClient == null) {
                    System.out.println("No message found");
                    count++;
                } else {

                    System.out.println("Got the message now respond " + messageFromClient.getJMSReplyTo()
                            + " " + messageFromClient.getJMSCorrelationID());
                    final MessageProducer producer = session.createProducer(messageFromClient.getJMSReplyTo());

                    System.out.println("Got the producer now respond ");

                    if (messageFromClient instanceof BytesMessage) {
                        final BytesMessage requestMessage = (BytesMessage) messageFromClient;

                        final int length = (int) requestMessage.getBodyLength();

                        final byte buffer[] = new byte[length];

                        requestMessage.readBytes(buffer);


                        final String message = new String(buffer, StandardCharsets.UTF_8);
                        final String responseText = "Server Got: " + message + " thank you";
                        final BytesMessage replyMessage = session.createBytesMessage();

                        replyMessage.setJMSCorrelationID(messageFromClient.getJMSCorrelationID());
                        replyMessage.writeBytes(responseText.getBytes(StandardCharsets.UTF_8));
                        producer.send(replyMessage);

                        System.out.println("SENT: " + responseText);
                    } else {
                        System.out.println("Message was not a bytes message " + messageFromClient);
                    }

                }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }


}

```
