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
