package io.nats.bridge.examples.ibmmq;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.JMSException;
import java.net.URL;

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

    public static JmsConnectionFactory createJmsConnectionFactoryWithNoQModel() throws JMSException {
        final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();


        final String HOST = "localhost";
        final int PORT = 1414;
        final String QUEUE_MANAGER = "QM1";
        final String CHANNEL = "DEV.APP.SVRCONN";
        final String USER = "app";
        final String PASSWORD = "passw0rd";


        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
        connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, PORT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER);
        connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        connectionFactory.setStringProperty(WMQConstants.USERID, USER);
        connectionFactory.setStringProperty(WMQConstants.PASSWORD, PASSWORD);

        return connectionFactory;
    }


    public static JmsConnectionFactory createJmsConnectionFactoryWithChannelTab() throws JMSException {
        final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();


        final String HOST = "localhost";
        final int PORT = 1414;
        final String PASSWORD = "passw0rd";
        final String USER = "app";
        final String QUEUE_MANAGER = "QM1";


        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
        connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, PORT);
        connectionFactory.setStringProperty(WMQConstants.USERID, USER);
        connectionFactory.setStringProperty(WMQConstants.PASSWORD, PASSWORD);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER);



        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);




        try {

            //copy tabl file from IBM docker image
            //      docker cp 08a3a58be213:/mnt/mqm/data/qmgrs/QM1/@ipcc/AMQCLCHL.TAB ~/synadia/nats-jms-bridge/AMQCLCHL.TAB
            //Using https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q032510_.htm as a guide
            //java.net.URL chanTab2 = new URL("ftp://ftp.server/admdata/ccdt2.tab"); //Example from IBM.
            //factory.setCCDTURL(chanTab2);
            //((MQConnectionFactory) connectionFactory).setCCDTURL(new URL("file://./ipcc/AMQCLCHL.TAB")); //Did not work
            //((MQConnectionFactory) connectionFactory).setCCDTURL(new URL("file:///./ipcc/AMQCLCHL.TAB")); //Did not work

            ((MQConnectionFactory) connectionFactory).setCCDTURL(new URL("file:///Users/richardhightower/synadia/nats-jms-bridge/examples/ipcc/AMQCLCHL.TAB"));



        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("unable to set CDTURL", ex);
        }

        return connectionFactory;
    }
}
