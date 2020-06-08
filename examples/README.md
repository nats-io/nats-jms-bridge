# Examples 

Included are simple examples to run the message bridge between JMS/IBM MQ and NATS using the JMS API and NATS. 



# Example 1: NATS to JMS Request/Reply Bridge


In the package `io.nats.bridge.examples.nats2jms` there are three files:

* `nats-bridge.yaml` sample config file 
* `SimpleJmsServer.java` simple JMS example that sends a response message.
* `SimpleNatsClient.java` simple NATS example that sends a NATS request. 



## SimpleJmsServer.java Walkthrough 

SimpleJmsServer create a `MessageConsumer` to listen for messages and respond. 

#### SimpleJmsServer.java - Create the `MessageConsumer` to listen for messages and respond.

```java 

            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils().withDestinationName("dynamicQueues/requests");

            final Session session = jmsBuildUtils.getSession();

            final MessageConsumer messageConsumer = jmsBuildUtils.getConsumerSupplier().get();

```


Next, `SimpleJmsServer` reads a message from the JMS queue. 

#### SimpleJmsServer.java - Read a message from the queue. 

```java
   final Message messageFromClient = messageConsumer.receive(waitForMessage.toMillis());
```


Then, `SimpleJmsServer` read the contents of the message, and creates the responseText.

#### SimpleJmsServer.java - Read the contents of the message, and responseText. 

```java 

    final BytesMessage requestMessage = (BytesMessage) messageFromClient;

    final int length =  (int) requestMessage.getBodyLength();

    final byte buffer[] = new byte[length];

    requestMessage.readBytes(buffer);


    final String message = new String(buffer, StandardCharsets.UTF_8);
    final String responseText = "Server Got: " + message + " thank you";


```

Lastly, `SimpleJmsServer` create a reply message, passes the response text and the JMS Correlation ID into the reply message. 
And then, `SimpleJmsServer` sends the reply message. 

#### SimpleJmsServer.java - Create a response message, passing the response text and the JMS Correlation ID

```java

    final BytesMessage replyMessage = session.createBytesMessage();
    replyMessage.setJMSCorrelationID(messageFromClient.getJMSCorrelationID());
    replyMessage.writeBytes(responseText.getBytes(StandardCharsets.UTF_8));
    producer.send(replyMessage);

```

## NATS client 
The NATS example client is simpler. It uses the request method on the NATS connection which expects a reply. 

#### SimpleNatsClient.java - The NATS example client is simpler
```java 

           final Options.Builder builder = new Options.Builder().server("nats://localhost:4222");
            final Connection connect = Nats.connect(builder.build());

            final Message replyFromJmsServer = connect.request("request_reply_jms",
                    "Hello World!".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(20));

            if (replyFromJmsServer != null) {
                System.out.println("RESPONSE FROM SERVER " + new String(replyFromJmsServer.getData(), StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message sent from JMS server");
            }
```

## Bridge config 
For these two work together, you need a JMS server and NATS running. 
Here is an example NATS JMS/IBM MQ Bridge config file that works for this example. 

#### nats-bridge.yaml - Bridge config for simple NATS to JMS example. 

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
- name: "nats2JMS"
  bridgeType: "REQUEST_REPLY"
  source:
    name: "nats"
    busType: "NATS"
    subject: "request_reply_jms"
    clusterName: "natsCluster"
  destination:
    name: "activeMQ"
    busType: "JMS"
    subject: "dynamicQueues/requests"
    clusterName: "jmsCluster"
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


```

# Example 2: JMS to NATS Request/Reply from JMS Client to NATS server 

There is not a lot new here. It is very similar to the first example but in the opposite direction. 


## SimpleNatsServer.java


The NATS SimpleNatsServer is very similar to the JMS version earlier, with the exception that it is using NATS
and not JMS. 

In the package `io.nats.bridge.examples.jms2Nats` there are three files:

* `nats-bridge.yaml` sample config file 
* `SimpleJmsClient.java` simple JMS example that sends a request message.
* `SimpleNatsServer.java` simple NATS example that sends a NATS reply message. 


#### SimpleNatsServer.java - simple NATS server 


```java
package io.nats.bridge.examples.jms2nats;

import io.nats.client.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleNatsServer {

    public static void main(final String[] args) {
        try {

            final Options.Builder builder = new Options.Builder().server("nats://localhost:4222");
            final Connection connect = Nats.connect(builder.build());
            final Subscription subscription = connect.subscribe("natsClientRequests");
            final Duration requestTimeoutDuration = Duration.ofSeconds(30);
            int count = 0;

            while (count < 100) {
                System.out.println("About to get Message");
                final Message messageFromClient = subscription.nextMessage(requestTimeoutDuration);
                System.out.println("Attempted to get Message");

                if (messageFromClient != null) {
                    System.out.println("Got the producer now respond ");
                    final byte buffer[] = messageFromClient.getData();
                    final String message = new String(buffer, StandardCharsets.UTF_8);
                    final String responseText = "Server Got: " + message + " thank you";
                    connect.publish(messageFromClient.getReplyTo(), responseText.getBytes(StandardCharsets.UTF_8));
                    System.out.println("SENT: " + responseText);
                } else {
                    System.out.println("No message found");
                    count++;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

```

## SimpleJmsClient 

The simple JMS client uses a temporary queue which is very common with JMS Request/Reply pattern. 
This example specifies a correlation ID and passes the response queue to the JMS message before sending it. 

## SimpleJmsClient.java - create message with correlation id, and response queue as the reply destination then send
```java 
package io.nats.bridge.examples.jms2nats;

import io.nats.bridge.examples.JmsBuildUtils;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class SimpleJmsClient {

    public static void main (String[] args) {
        try {

            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils().withDestinationName("dynamicQueues/client-requests");
            final Session session = jmsBuildUtils.getSession();
            final MessageProducer messageProducer = jmsBuildUtils.getProducerSupplier().get();
            final Destination replyQueue = session.createTemporaryQueue();
            final Duration replyTimeoutDuration = Duration.ofSeconds(30);

            final BytesMessage requestMessage = session.createBytesMessage();
            requestMessage.writeBytes("Hello World!".getBytes(StandardCharsets.UTF_8));
            requestMessage.setJMSCorrelationID(UUID.randomUUID().toString());
            requestMessage.setJMSReplyTo(replyQueue);

            messageProducer.send(requestMessage);

            final MessageConsumer replyConsumer = session.createConsumer(replyQueue);

            final Message reply = replyConsumer.receive(replyTimeoutDuration.toMillis());

            if (reply instanceof BytesMessage) {
                final BytesMessage bytesReply = (BytesMessage) reply;
                final byte[] buffer = new  byte[(int)bytesReply.getBodyLength()];
                bytesReply.readBytes(buffer);

                System.out.println("REPLY: " + new String(buffer, StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message came back or wrong type");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

```

## Bridge Config 

The bridge config sets up the subject and queue names for this example. 

## nats-bridge.yaml - Bridge Config for JMS to NATS Request/reply bridge 

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
- name: "jms2Nats"
  bridgeType: "REQUEST_REPLY"
  source:
    name: "activeMQ"
    busType: "JMS"
    subject: "dynamicQueues/client-requests"
    clusterName: "jmsCluster"
  destination:
    name: "nats"
    busType: "NATS"
    subject: "natsClientRequests"
    clusterName: "natsCluster"
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

```

