package io.nats.bridge.ibmmq;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import io.nats.bridge.MessageBus;
import io.nats.bridge.integration.TestUtils;
import io.nats.bridge.messages.Message;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.*;

public class BasicIbmMqIntegrationTest {

    private MessageBus serverMessageBus;
    private MessageBus clientMessageBus;

    @Before
    public void setUp() throws Exception {



    }

    @Test
    public void testBasic() throws Exception {
        final String host ="localhost";
        final int port = 1414;
        final String user= "app";
        final int timeout = 15000; // in ms or 15 seconds
        final String channel = "DEV.APP.SVRCONN";
        final String password = "passw0rd";
        final String queueManagerName = "QM1";
        final String destinationName= "DEV.QUEUE.1";

        final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        final JmsConnectionFactory cf = ff.createConnectionFactory();

        // Set the properties
        cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
        cf.setIntProperty(WMQConstants.WMQ_PORT, port);
        cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);

        cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);

        cf.setStringProperty(WMQConstants.USERID, user);
        cf.setStringProperty(WMQConstants.PASSWORD, password);
        cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);


        // Create JMS objects
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(destinationName);
        MessageConsumer consumer = session.createConsumer(destination);

        // Start the connection
        //connection.start();
    }

    @Test
    public void testLib() throws Exception {
        final Hashtable<String, Object> jndiProperties = new Hashtable<>();
        jndiProperties.put("java.naming.factory.initial", System.getenv().getOrDefault("NATS_BRIDGE_JMS_NAMING_FACTORY", "io.nats.bridge.ibmmq.IbmMqInitialContextFactory"));
        jndiProperties.put("nats.ibm.mq.host", System.getenv().getOrDefault("NATS_BRIDGE_JMS_CONNECTION_FACTORY", "tcp://localhost:1414"));
        jndiProperties.put("nats.ibm.mq.channel", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_CHANNEL", "DEV.APP.SVRCONN"));
        jndiProperties.put("nats.ibm.mq.queueManager", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_QUEUE_MANAGER", "QM1"));
        final InitialContext context = new InitialContext(jndiProperties);
        final Object connectionFactoryObject = context.lookup("ConnectionFactory");
        assertTrue(connectionFactoryObject instanceof IbmMqInitialContextFactory.MQConnectionFactory);
        final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryObject;
        final Connection connection = connectionFactory.createConnection("app", "passw0rd");
        connection.start();
        final Session session = connection.createSession();
        final Destination destination = session.createQueue("DEV.QUEUE.1");
        final Destination responseDestination = session.createQueue("DEV.QUEUE.2");
        final MessageProducer producer = session.createProducer(destination);
        final MessageConsumer consumer = session.createConsumer(destination);

        while (consumer.receive(100)!=null) {
            System.out.println("drain");
        }

        producer.send(session.createTextMessage("Hello"));
        final javax.jms.Message message = consumer.receive(1000);

        System.out.println(message);

        assertTrue(message instanceof TextMessage);

        final TextMessage textMessage = (TextMessage) message;

        assertEquals("Hello", textMessage.getText());


        final String correlationID = UUID.randomUUID().toString();
        final TextMessage requestMessage = session.createTextMessage("REQUEST");
        requestMessage.setJMSReplyTo(responseDestination);
        requestMessage.setJMSCorrelationID(correlationID);
        producer.send(requestMessage);

        //Act like Server
        final MessageConsumer serverConsumer = session.createConsumer(destination);
        final TextMessage requestFromClient = (TextMessage) serverConsumer.receive(5000);
        assertEquals("REQUEST", requestFromClient.getText());
        final Destination replyToDestination = requestFromClient.getJMSReplyTo();
        final TextMessage replyMessage = session.createTextMessage("RESPONSE_FROM_SERVER");
        replyMessage.setJMSCorrelationID(requestFromClient.getJMSCorrelationID());
        session.createProducer(replyToDestination).send(replyMessage);


        //Act like original client
        final TextMessage replyFromServer = (TextMessage) session.createConsumer(responseDestination).receive(5000);
        assertEquals("RESPONSE_FROM_SERVER", replyFromServer.getText());


        connection.stop();

    }

    @Test
    public void test() throws Exception {

        clientMessageBus = TestUtils.getMessageBusIbmMQ("", true);
        serverMessageBus = TestUtils.getMessageBusIbmMQ("", true );

        clientMessageBus.publish("hello");



        final Optional<Message> message = serverMessageBus.receive(Duration.ofSeconds(5));


        assertTrue(message.isPresent());
        final String result = message.get().bodyAsString();
        assertEquals("hello", result);
    }
}
