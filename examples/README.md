# Examples 

Included are simple examples to run the message bridge between JMS/IBM MQ and NATS using the JMS API and NATS. 



# Example 1: NATS to JMS 


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

