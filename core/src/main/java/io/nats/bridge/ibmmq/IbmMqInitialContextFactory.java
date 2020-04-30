package io.nats.bridge.ibmmq;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import java.net.URI;
import java.util.*;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;


public class IbmMqInitialContextFactory implements InitialContextFactory {

    private final Map<String, Object> contextMap = new HashMap<String, Object>();
    private final static String CONNECTION_FACTORY = "ConnectionFactory";
    private final static String PREFIX = "nats.ibm.mq.";
    private final static String HOST = PREFIX + "host";
    private final static String CHANNEL = PREFIX + "channel";
    private final static String QUEUE_MANAGER = PREFIX + "queueManager";
    //private final static String QUEUE =  "queue.";


    @Override
    public Context getInitialContext(final Hashtable<?, ?> jndiProperties) throws NamingException {

        try {
            final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();

            final String hostURL = getStringProp(jndiProperties, HOST);
            final URI uri = new URI(hostURL);
            final String host = uri.getHost();
            final int port = uri.getPort();
            final String channel = getStringProp(jndiProperties, CHANNEL);
            final String queueManagerName = getStringProp(jndiProperties, QUEUE_MANAGER);

            connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
            connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, port);
            connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
            connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            contextMap.put(CONNECTION_FACTORY, new MQConnectionFactory(connectionFactory));

//            final Set<String> queues = jndiProperties.keySet().stream().map(Object::toString)
//                    .filter(s -> s.startsWith(QUEUE)).collect(Collectors.toSet());


            return new MQContext(contextMap);
        } catch (Exception ex) {
            throw new NamingException(ex.getLocalizedMessage()); //TODO something better than this
        }
    }


    private String getStringProp(Hashtable<?, ?> jndiProperties, String key) {
        if (!jndiProperties.containsKey(key)) throw new IllegalStateException("REQ KEY IS MISSING KEY " + key);
        return (String) jndiProperties.get(key);
    }

    public static class MQConnectionFactory implements ConnectionFactory {

        private final JmsConnectionFactory connectionFactory;


        private MQConnectionFactory(final JmsConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;

        }

        @Override
        public Connection createConnection() throws JMSException {
            return connectionFactory.createConnection();
        }

        @Override
        public Connection createConnection(final String userName, final String password) throws JMSException {
            connectionFactory.setStringProperty(WMQConstants.USERID, userName);
            connectionFactory.setStringProperty(WMQConstants.PASSWORD, password);
            return connectionFactory.createConnection(userName, password);
        }

        @Override
        public JMSContext createContext() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    public static class MQContext implements Context {


        final Map<String, Object> contextMap;

        private MQContext(Map<String, Object> contextMap) {
            this.contextMap = contextMap;
        }

        @Override
        public Object lookup(final String name) throws NamingException {
            if (name == null || !contextMap.containsKey(name)) throw new NamingException("Name " + name + " not found");

            return contextMap.get(name);
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }


        @Override
        public void bind(Name name, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void bind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rebind(Name name, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rebind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void unbind(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void unbind(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rename(Name name, Name name1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void rename(String s, String s1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void destroySubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Context createSubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object lookupLink(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public NameParser getNameParser(String s) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Name composeName(Name name, Name name1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public String composeName(String s, String s1) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object addToEnvironment(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Object removeFromEnvironment(String s) throws NamingException {
            return null;
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void close() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
