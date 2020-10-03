package io.nats.bridge.integration.ibmmq;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import io.nats.bridge.tls.SslContextBuilderException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;

public class IbmMqInitialContextFactory implements InitialContextFactory {

    private final Map<String, Object> contextMap = new HashMap<String, Object>();
    private final static String CONNECTION_FACTORY = "ConnectionFactory";
    public final static String PREFIX = "nats.ibm.mq.";
    private final static String HOST = PREFIX + "host";
    private final static String CHANNEL = PREFIX + "channel";
    private final static String CCDT_URL = PREFIX + "ccdtUrl";
    private final static String QUEUE_MANAGER = PREFIX + "queueManager";
    private final static String QUEUE_MODEL_NAME = PREFIX + "queueModelName";
    private final static String QUEUE_MODEL_PREFIX = PREFIX + "queueModelPrefix";
    public final static String KEY_STORE_PASS_BASE64_ENV = PREFIX + "keystorePass";
    public final static String TRUST_STORE_PASS_BASE64_ENV = PREFIX + "truststorePass";

    public static void initSSL(final Hashtable<?, ?> jndiProperties, final Properties properties,
                               final Map<String, String> env) {
        final String keystoreBase64Env = getOptionalStringPropWithDefault(jndiProperties,
                KEY_STORE_PASS_BASE64_ENV, "KEYSTORE_PASSWORD");
        final String trustStoreBase64Env = getOptionalStringPropWithDefault(jndiProperties,
                TRUST_STORE_PASS_BASE64_ENV, "TRUSTSTORE_PASSWORD");
        final String keystorePasswordBase64 = env.get(keystoreBase64Env);
        final String trustStorePasswordBase64 = env.get(trustStoreBase64Env);
        if (keystorePasswordBase64 != null) {
            String keystorePassword = new String(Base64.getDecoder().decode(keystorePasswordBase64), StandardCharsets.UTF_8);
            properties.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        }
        if (trustStorePasswordBase64 != null) {
            String trustStorePassword = new String(Base64.getDecoder().decode(trustStorePasswordBase64), StandardCharsets.UTF_8);
            properties.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        }
    }

    @Override
    public Context getInitialContext(final Hashtable<?, ?> jndiProperties) throws NamingException {
        try {
            initSSL(jndiProperties, System.getProperties(), System.getenv());

            final JmsFactoryFactory factoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            final JmsConnectionFactory connectionFactory = factoryFactory.createConnectionFactory();


            final String ccdtURL = getOptionalStringProp(jndiProperties, CCDT_URL);
            if (ccdtURL!=null) {
                if (connectionFactory instanceof MQConnectionFactory) {
                    final MQConnectionFactory factory = (MQConnectionFactory) connectionFactory;
                    factory.setCCDTURL(new URL(ccdtURL));
                } else {
                    System.out.println("Unable to set CCDT file using " + CCDT_URL);
                }
            } else {
                final String hostURL = getOptionalStringProp(jndiProperties, HOST);
                final String channel = getStringProp(jndiProperties, CHANNEL);
                if (hostURL != null) {
                    final URI uri = new URI(hostURL);
                    final String host = uri.getHost();
                    final int port = uri.getPort();
                    connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
                    connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, port);
                }
                if (channel!=null) {
                    connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
                }
                connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            }


            final String queueManagerName = getStringProp(jndiProperties, QUEUE_MANAGER);
            final String queueModelName = getOptionalStringProp(jndiProperties, QUEUE_MODEL_NAME);
            final String queueModelPrefix = getOptionalStringProp(jndiProperties, QUEUE_MODEL_PREFIX);


            connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
            connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);

            final List<PropertyValue> propertyValues = PropertyUtils.extractProperties((Hashtable<String, String>) jndiProperties);

            PropertyUtils.initJMSContext(connectionFactory, propertyValues);

            if (queueModelName != null)
                connectionFactory.setStringProperty(WMQConstants.WMQ_TEMPORARY_MODEL, queueModelName);

            if (queueModelPrefix != null)
                connectionFactory.setStringProperty(WMQConstants.WMQ_TEMP_Q_PREFIX, queueModelPrefix);

            contextMap.put(CONNECTION_FACTORY, new NatsMQConnectionFactory(connectionFactory));

            return new MQContext(contextMap);
        } catch (Exception ex) {
            throw new NamingException(ex.getLocalizedMessage()); //TODO something better than this
        }
    }

    @SuppressWarnings("unchecked")
    private static  String getOptionalStringPropWithDefault(Hashtable<?, ?> map, String key, String defaultStr) {
        final Map<String, String> jndiProperties = (Map<String, String>) map;
        return (String) jndiProperties.getOrDefault(key, defaultStr);
    }

    private String getOptionalStringProp(Hashtable<?, ?> jndiProperties, String key) {
        return (String) jndiProperties.get(key);
    }

    private String getStringProp(Hashtable<?, ?> jndiProperties, String key) {
        if (!jndiProperties.containsKey(key)) throw new IllegalStateException("REQ KEY IS MISSING KEY " + key);
        return (String) jndiProperties.get(key);
    }

    public static class NatsMQConnectionFactory implements ConnectionFactory {

        private final JmsConnectionFactory connectionFactory;


        private NatsMQConnectionFactory(final JmsConnectionFactory connectionFactory) {
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
