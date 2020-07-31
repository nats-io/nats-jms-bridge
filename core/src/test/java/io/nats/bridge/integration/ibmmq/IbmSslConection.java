package io.nats.bridge.integration.ibmmq;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

public class IbmSslConection {
    private MessageBus serverMessageBus;
    private MessageBus clientMessageBus;

    @Before
    public void setUp() throws Exception {


    }

    public void testSendMessageWithDynamicQueueTls() throws Exception {

        try {
            final String hostURI = System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_HOST", "tcp://localhost:1414");
            final URI uri = new URI(hostURI);
            final String host = uri.getHost();
            final int port = uri.getPort();
            final String user = "app";
            final int timeout = 15000; // in ms or 15 seconds
            final String channel = "DEV.APP.SVRCONN";
            final String password = "passw0rd";
            final String queueManagerName = "QM1";
            final String destinationName = "DEV.QUEUE.1";
            final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            final JmsConnectionFactory cf = ff.createConnectionFactory();
            final String sslkeyr ="../certs/clientibmkey.kdb" ;
            final String sslpass ="cloudurable";
            // Set the properties

            System.setProperty("java.net.ssl.trustStore", sslkeyr);
            System.setProperty("javax.net.ssl.keyStore", sslkeyr );
            System.setProperty("javax.net.ssl.keyStorePassword", sslpass );

            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
            cf.setIntProperty(WMQConstants.WMQ_PORT, port);
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
            cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TLS_RSA_WITH_AES_128_CBC_SHA256");
            //cf.setStringProperty(WMQConstants.WMQ_SSL_FIPS_REQUIRED, "false");
            cf.setStringProperty(WMQConstants.USERID, user);
            cf.setStringProperty(WMQConstants.PASSWORD, password);
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            cf.setStringProperty(WMQConstants.WMQ_TEMPORARY_MODEL, "DEV.MODEL");
            cf.setStringProperty(WMQConstants.WMQ_TEMP_Q_PREFIX, "DEV*");

            // Create JMS objects
            final Connection connection = cf.createConnection();
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            final Destination destination = session.createQueue(destinationName);
            Destination responseDestination = null;

            try {
                responseDestination = session.createTemporaryQueue();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }

            final MessageConsumer consumer = session.createConsumer(destination);

            // Start the connection
            connection.start();

            final MessageProducer producer = session.createProducer(destination);
            while (consumer.receive(100) != null) {
                System.out.println("drain");
            }
            final String correlationID = UUID.randomUUID().toString();

            final TextMessage requestMessage = session.createTextMessage("REQUEST");
            requestMessage.setJMSReplyTo(responseDestination);
            requestMessage.setJMSCorrelationID(correlationID);

            producer.send(requestMessage); //Send
            final MessageConsumer serverConsumer = session.createConsumer(destination);
            final TextMessage requestFromClient = (TextMessage) serverConsumer.receive(5000); //pretend you are the server


            assertEquals("REQUEST", requestFromClient.getText());

            //Server replying
            final Destination replyToDestination = requestFromClient.getJMSReplyTo();
            final TextMessage replyMessage = session.createTextMessage("RESPONSE_FROM_SERVER");
            replyMessage.setJMSCorrelationID(requestFromClient.getJMSCorrelationID());
            session.createProducer(replyToDestination).send(replyMessage);

            //Act like original client
            final TextMessage replyFromServer = (TextMessage) session.createConsumer(responseDestination).receive(5000);
            assertEquals("RESPONSE_FROM_SERVER", replyFromServer.getText());
            connection.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }
}
