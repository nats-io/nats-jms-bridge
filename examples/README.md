# Examples 

Included are simple examples to run the message bridge between JMS/IBM MQ and NATS using the JMS API and NATS. 


* [Simple NATS to JMS Request / Reply example](#example-1-nats-to-jms-requestreply-bridge)
* [Simple JMS to NATA Request/ Reply example](#example-2-jms-to-nats-requestreply-from-jms-client-to-nats-server)
* [Simple NATS to IBM MQ Request / Reply example using QModel](#example-3-nats-to-ibm-mq-requestreply-bridge-with-qmodel)
* [Simple IBM MQ to NATA Request/ Reply example using QModel](#example-4-ibm-mq-to-nats-requestreply-from-jms-client-to-nats-server-using-qmodel)


# Example 1: NATS to JMS Request/Reply Bridge


In the package `io.nats.bridge.examples.jms.nats2jms` there are three files:

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

In the package `io.nats.bridge.examples.jms.jms2Nats` there are three files:

* `nats-bridge.yaml` sample config file 
* `SimpleJmsClient.java` simple JMS example that sends a request message.
* `SimpleNatsServer.java` simple NATS example that sends a NATS reply message. 


#### SimpleNatsServer.java - simple NATS server 


```java
package io.nats.bridge.examples.jms.jms2nats;

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
package io.nats.bridge.examples.jms.jms2nats;

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

____




# Example 3: NATS to IBM MQ Request/Reply Bridge with QModel


In the package `io.nats.bridge.examples.ibmmq.nats2mq` there are four files:

* `nats-bridge.yaml` sample config file for NATS to IBM MQ
* `ibm.mqsc` IBM MQ Config file that configures the Queues and the Queue model 
* `SimpleMqServer.java` simple IBM MQ example that sends a response message.
* `SimpleNatsClient.java` simple NATS example that sends a NATS request. 



## SimpleMqServer.java Walkthrough 

`SimpleMqServer` create a `MessageConsumer` to listen for messages and respond. 

#### SimpleMqServer.java - Create the `MessageConsumer` to listen for messages and respond.

```java 

            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils()
                    .withConnectionFactory(IbmMqUtils.createJmsConnectionFactoryWithQModel());


            final Session session = jmsBuildUtils.getSession();


            jmsBuildUtils.withDestination(session.createQueue("DEV.QUEUE.1"));


            final MessageConsumer messageConsumer = jmsBuildUtils.getConsumerSupplier().get();

```


Notice to create the builder, the `SimpleMqServer` creates an IBM JMSConnectionFactory as follows and passed it with `withConnectionFactory`.

This is because IBM MQ's JMS support does not support remote clients with JNDI like most JMS remote clients do. 


#### SimpleMqServer - create JMS Connection Factory 

```java

package io.nats.bridge.examples.ibmmq;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.JMSException;

public class IbmMqUtils {

    public static JmsConnectionFactory createJmsConnectionFactoryWithQModel() throws JMSException {
        final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();


        final String HOST = "localhost";
        final int PORT = 1414;
        final String QUEUE_MANAGER = "QM1";
        final String QUEUE_MODEL = "DEV.MODEL";
        final String QUEUE_MODEL_PREFIX = "DEV*";
        final String CHANNEL = "DEV.APP.SVRCONN";
        final String USER = "app";
        final String PASSWORD = "passw0rd";


        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
        connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, PORT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER);
        connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        connectionFactory.setStringProperty(WMQConstants.WMQ_TEMPORARY_MODEL, QUEUE_MODEL);
        connectionFactory.setStringProperty(WMQConstants.WMQ_TEMP_Q_PREFIX, QUEUE_MODEL_PREFIX);
        connectionFactory.setStringProperty(WMQConstants.USERID, USER);
        connectionFactory.setStringProperty(WMQConstants.PASSWORD, PASSWORD);

        return connectionFactory;
    }
}


```




Next, `SimpleMQServer` reads a message from the JMS queue. 

#### SimpleMQServer.java - Read a message from the queue. 

```java
   final Message messageFromClient = messageConsumer.receive(waitForMessage.toMillis());
```


Then, `SimpleMQServer` read the contents of the message, and creates the responseText.

#### SimpleMQServer.java - Read the contents of the message, and responseText. 

```java 

    final BytesMessage requestMessage = (BytesMessage) messageFromClient;

    final int length =  (int) requestMessage.getBodyLength();

    final byte buffer[] = new byte[length];

    requestMessage.readBytes(buffer);


    final String message = new String(buffer, StandardCharsets.UTF_8);
    final String responseText = "Server Got: " + message + " thank you";


```

Lastly, `SimpleMQServer` create a reply message, passes the response text and the JMS Correlation ID into the reply message. 
And then, `SimpleMQServer` sends the reply message. 

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

            final Message replyFromJmsServer = connect.request("request_reply_ibmmq",
                    "Hello World!".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(20));

            if (replyFromJmsServer != null) {
                System.out.println("RESPONSE FROM SERVER " + new String(replyFromJmsServer.getData(), StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message sent from JMS server");
            }
```

## Bridge config 
For these two to work together, you need a IBM MQ server and NATS running. 
Here is an example NATS JMS/IBM MQ Bridge config file that works for this example. 

#### nats-bridge.yaml - Bridge config for simple NATS to IBM MQ example. 

```yaml 
---
name: "NATS to IBM MQ Request/Reply Example"
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
      subject: "request_reply_ibmmq"
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

Lastly, you need to configure IBM MQ with the correct QModel and Queue for this example to work. 
Here is a sample IBM MQ config file. 

```shell script
* Developer queues config for IBM MQ
DEFINE QLOCAL('DEV.QUEUE.1') REPLACE
DEFINE QLOCAL('DEV.QUEUE.2') REPLACE
DEFINE QLOCAL('DEV.QUEUE.3') REPLACE
DEFINE QLOCAL('DEV.DEAD.LETTER.QUEUE') REPLACE
* Creating a model queue to dynamic queues
* https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.1.0/com.ibm.mq.ref.dev.doc/prx_wmq_tempy_model.htm
* https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q032240_.htm
DEFINE QMODEL('DEV.MODEL') REPLACE
* Use a different dead letter queue, for undeliverable messages
ALTER QMGR DEADQ('DEV.DEAD.LETTER.QUEUE')
* Developer topics
DEFINE TOPIC('DEV.BASE.TOPIC') TOPICSTR('dev/') REPLACE
* Developer connection authentication
DEFINE AUTHINFO('DEV.AUTHINFO') AUTHTYPE(IDPWOS) CHCKCLNT(REQDADM) CHCKLOCL(OPTIONAL) ADOPTCTX(YES) REPLACE
ALTER QMGR CONNAUTH('DEV.AUTHINFO')
REFRESH SECURITY(*) TYPE(CONNAUTH)
* Developer channels (Application + Admin)
* Developer channels (Application + Admin)
DEFINE CHANNEL('DEV.ADMIN.SVRCONN') CHLTYPE(SVRCONN) REPLACE
DEFINE CHANNEL('DEV.APP.SVRCONN') CHLTYPE(SVRCONN) MCAUSER('app') REPLACE
* Developer channel authentication rules
SET CHLAUTH('*') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(NOACCESS) DESCR('Back-stop rule - Blocks everyone') ACTION(REPLACE)
SET CHLAUTH('DEV.APP.SVRCONN') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(CHANNEL) CHCKCLNT(ASQMGR) DESCR('Allows connection via APP channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(BLOCKUSER) USERLIST('nobody') DESCR('Allows admins on ADMIN channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(USERMAP) CLNTUSER('admin') USERSRC(CHANNEL) DESCR('Allows admin user to connect via ADMIN channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(USERMAP) CLNTUSER('admin') USERSRC(MAP) MCAUSER ('mqm') DESCR ('Allow admin as MQ-admin') ACTION(REPLACE)
* Developer authority records
SET AUTHREC PRINCIPAL('app') OBJTYPE(QMGR) AUTHADD(CONNECT,INQ)
SET AUTHREC PROFILE('DEV.**') PRINCIPAL('app') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT,DSP)
SET AUTHREC PROFILE('DEV.**') PRINCIPAL('app') OBJTYPE(TOPIC) AUTHADD(PUB,SUB)

```

____



# Example 4: IBM MQ to NATS Request/Reply from JMS Client to NATS server using QModel

There is not a lot new here. It is very similar to the last example but in the opposite direction. 


## SimpleNatsServer.java


The NATS SimpleNatsServer is very similar to the JMS version earlier, with the exception that it is using NATS
and not JMS and IBM MQ. 

In the package `io.nats.bridge.examples.ibmmq.mq2nats` there are three files:

* `nats-bridge.yaml` sample config file 
* `SimpleMQClient.java` simple IBM MQ / JMS example that sends a request message.
* `SimpleNatsServer.java` simple NATS example that sends a NATS reply message. 


#### SimpleNatsServer.java - simple NATS server 


```java
package io.nats.bridge.examples.jms.jms2nats;

import io.nats.client.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleNatsServer {

    public static void main(final String[] args) {
        try {

            final Options.Builder builder = new Options.Builder().server("nats://localhost:4222");
            final Connection connect = Nats.connect(builder.build());
            final Subscription subscription = connect.subscribe("reply_mq");
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

## SimpleMQClient 

The simple MQ client uses a temporary queue which is very common with JMS Request/Reply pattern. 
This uses the QModel and QModel pattern which is shown in the `IbmMqUtils.createJmsConnectionFactoryWithQModel` method.
This example specifies a correlation ID and passes the response queue to the JMS message before sending it. 

## SimpleMQClient.java - create message with correlation id, and response queue as the reply destination then send
```java 
package io.nats.bridge.examples.ibmmq.mq2nats;

import io.nats.bridge.examples.JmsBuildUtils;
import io.nats.bridge.examples.ibmmq.IbmMqUtils;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class SimpleMqClient {

    public static void main (String[] args) {
        try {


            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils()
                    .withConnectionFactory(IbmMqUtils.createJmsConnectionFactoryWithQModel());
            final Session session = jmsBuildUtils.getSession();
            jmsBuildUtils.withDestination(session.createQueue("DEV.QUEUE.2"));


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
name: "IBM MQ to NATS Request/Reply Example"
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
      name: "ibmMQ"
      busType: "JMS"
      subject: "DEV.QUEUE.2"
      clusterName: "ibmMqCluster"
    destination:
      name: "nats"
      busType: "NATS"
      subject: "reply_mq"
      clusterName: "natsCluster"

    copyHeaders: false
    workers: 5
    tasks : 2

clusters:
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

Lastly, you need to configure IBM MQ with the correct QModel and Queue for this example to work. 
Here is a sample IBM MQ config file. 

```shell script
* Developer queues config for IBM MQ
DEFINE QLOCAL('DEV.QUEUE.1') REPLACE
DEFINE QLOCAL('DEV.QUEUE.2') REPLACE
DEFINE QLOCAL('DEV.QUEUE.3') REPLACE
DEFINE QLOCAL('DEV.DEAD.LETTER.QUEUE') REPLACE
* Creating a model queue to dynamic queues
* https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.1.0/com.ibm.mq.ref.dev.doc/prx_wmq_tempy_model.htm
* https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q032240_.htm
DEFINE QMODEL('DEV.MODEL') REPLACE
* Use a different dead letter queue, for undeliverable messages
ALTER QMGR DEADQ('DEV.DEAD.LETTER.QUEUE')
* Developer topics
DEFINE TOPIC('DEV.BASE.TOPIC') TOPICSTR('dev/') REPLACE
* Developer connection authentication
DEFINE AUTHINFO('DEV.AUTHINFO') AUTHTYPE(IDPWOS) CHCKCLNT(REQDADM) CHCKLOCL(OPTIONAL) ADOPTCTX(YES) REPLACE
ALTER QMGR CONNAUTH('DEV.AUTHINFO')
REFRESH SECURITY(*) TYPE(CONNAUTH)
* Developer channels (Application + Admin)
* Developer channels (Application + Admin)
DEFINE CHANNEL('DEV.ADMIN.SVRCONN') CHLTYPE(SVRCONN) REPLACE
DEFINE CHANNEL('DEV.APP.SVRCONN') CHLTYPE(SVRCONN) MCAUSER('app') REPLACE
* Developer channel authentication rules
SET CHLAUTH('*') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(NOACCESS) DESCR('Back-stop rule - Blocks everyone') ACTION(REPLACE)
SET CHLAUTH('DEV.APP.SVRCONN') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(CHANNEL) CHCKCLNT(ASQMGR) DESCR('Allows connection via APP channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(BLOCKUSER) USERLIST('nobody') DESCR('Allows admins on ADMIN channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(USERMAP) CLNTUSER('admin') USERSRC(CHANNEL) DESCR('Allows admin user to connect via ADMIN channel') ACTION(REPLACE)
SET CHLAUTH('DEV.ADMIN.SVRCONN') TYPE(USERMAP) CLNTUSER('admin') USERSRC(MAP) MCAUSER ('mqm') DESCR ('Allow admin as MQ-admin') ACTION(REPLACE)
* Developer authority records
SET AUTHREC PRINCIPAL('app') OBJTYPE(QMGR) AUTHADD(CONNECT,INQ)
SET AUTHREC PROFILE('DEV.**') PRINCIPAL('app') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT,DSP)
SET AUTHREC PROFILE('DEV.**') PRINCIPAL('app') OBJTYPE(TOPIC) AUTHADD(PUB,SUB)

```
Recall that the IbmMQUtils.createJmsConnectionFactoryWithQModel uses the Queue and QModel from the above IBM config. 

#### IbmMqUtils

```java
package io.nats.bridge.examples.ibmmq;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.JMSException;

public class IbmMqUtils {

    public static JmsConnectionFactory createJmsConnectionFactoryWithQModel() throws JMSException {
        final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();


        final String HOST = "localhost";
        final int PORT = 1414;
        final String QUEUE_MANAGER = "QM1";
        final String QUEUE_MODEL = "DEV.MODEL";
        final String QUEUE_MODEL_PREFIX = "DEV*";
        final String CHANNEL = "DEV.APP.SVRCONN";
        final String USER = "app";
        final String PASSWORD = "passw0rd";


        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
        connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, PORT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER);
        connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        connectionFactory.setStringProperty(WMQConstants.WMQ_TEMPORARY_MODEL, QUEUE_MODEL);
        connectionFactory.setStringProperty(WMQConstants.WMQ_TEMP_Q_PREFIX, QUEUE_MODEL_PREFIX);
        connectionFactory.setStringProperty(WMQConstants.USERID, USER);
        connectionFactory.setStringProperty(WMQConstants.PASSWORD, PASSWORD);

        return connectionFactory;
    }
}

```