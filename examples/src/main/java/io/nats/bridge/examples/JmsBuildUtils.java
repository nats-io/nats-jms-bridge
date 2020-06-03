package io.nats.bridge.examples;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.IllegalStateException;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class JmsBuildUtils {

    private ConnectionFactory connectionFactory;
    private Destination destination;
    private Session session;
    private Connection connection;
    private String connectionFactoryName = "ConnectionFactory";
    private String destinationName;
    private int acknowledgeSessionMode = Session.AUTO_ACKNOWLEDGE;
    private boolean transactionalSession = false;
    private Function<Connection, Session> sessionCreator;
    private Function<ConnectionFactory, Connection> connectionCreator;
    private String userNameConnection;
    private String passwordConnection;
    private Context context;

    private Supplier<MessageConsumer> consumerSupplier;
    private MessageConsumer responseConsumer;

    private Destination responseDestination;
    private Hashtable<String, Object> jndiProperties = new Hashtable<>();
    private boolean ibmMQ = false;
    private String responseDestinationName = "TEMP_QUEUE";

    private Supplier<MessageProducer> producerSupplier;

    private String ibmMQQueueModelName;
    private String ibmMQQueueModelPrefix;
    private String ibmMQQueueManager;
    private String ibmMQChannel;

    private boolean requestReply = true;

    public boolean isRequestReply() {
        return requestReply;
    }

    public JmsBuildUtils withRequestReply(boolean requestReply) {
        this.requestReply = requestReply;
        return this;
    }

    public static JmsBuildUtils builder() {
        return new JmsBuildUtils();
    }

    public String getIbmMQQueueModelName() {

        if (ibmMQQueueModelName == null) {
            ibmMQQueueModelName = System.getenv().getOrDefault("NATS_BRIDGE_IBM_QUEUE_MODEL_NAME", "DEV.MODEL");


        }
        return ibmMQQueueModelName;
    }

    public JmsBuildUtils withIbmMQQueueModelName(String ibmMQQueueModelName) {
        this.ibmMQQueueModelName = ibmMQQueueModelName;
        return this;
    }

    public String getIbmMQQueueModelPrefix() {
        if (ibmMQQueueModelPrefix == null) {
            ibmMQQueueModelPrefix = System.getenv().getOrDefault("NATS_BRIDGE_IBM_QUEUE_MODEL_PREFIX", "DEV*");
        }
        return ibmMQQueueModelPrefix;
    }

    public JmsBuildUtils withIbmMQQueueModelPrefix(String ibmMQQueueModelPrefix) {
        this.ibmMQQueueModelPrefix = ibmMQQueueModelPrefix;
        return this;
    }

    public String getIbmMQQueueManager() {
        if (ibmMQQueueManager == null) {
            ibmMQQueueManager = System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_QUEUE_MANAGER", "QM1");
        }
        return ibmMQQueueManager;
    }

    public JmsBuildUtils withIbmMQQueueManager(String ibmMQQueueManager) {
        this.ibmMQQueueManager = ibmMQQueueManager;
        return this;
    }

    public String getIbmMQChannel() {
        if (ibmMQChannel == null) {
            ibmMQChannel = System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_CHANNEL", "DEV.APP.SVRCONN");
        }
        return ibmMQChannel;
    }

    public JmsBuildUtils withIbmMQChannel(String ibmMQChannel) {
        this.ibmMQChannel = ibmMQChannel;
        return this;
    }



    public JmsBuildUtils withJndiProperty(String name, String value) {
        this.getJndiProperties().put(name, value);
        return this;
    }

    public JmsBuildUtils withJndiProperties(Map<String, String> props) {
        this.getJndiProperties().putAll(props);
        return this;
    }


    public Supplier<MessageProducer> getProducerSupplier() {
        if (producerSupplier == null) {

            producerSupplier = () -> {
                try {
                    return getSession().createProducer(getDestination());
                } catch (JMSException e) {
                    throw new IllegalStateException(e);
                }
            };

        }
        return producerSupplier;
    }

    public void withProducerSupplier(final Supplier<MessageProducer> producerSupplier) {
        this.producerSupplier = producerSupplier;
    }


    public Supplier<MessageConsumer> getConsumerSupplier() {

        if (consumerSupplier == null) {
            consumerSupplier = () -> {
                try {
                    return getSession().createConsumer(getDestination());
                } catch (JMSException e) {
                    throw new IllegalStateException(e);
                }
            };
        }

        return consumerSupplier;
    }

    public void withConsumerSupplier(final Supplier<MessageConsumer> consumerSupplier) {
        this.consumerSupplier = consumerSupplier;
    }


    public Destination getResponseDestination() {
        if (responseDestination == null) {
            try {
                if ((getResponseDestinationName().equals("TEMP_QUEUE"))) {
                    responseDestination = getSession().createTemporaryQueue();
                } else {
                    responseDestination = getSession().createQueue(getResponseDestinationName());
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return responseDestination;
    }

    public JmsBuildUtils withResponseDestination(Destination responseDestination) {
        this.responseDestination = responseDestination;
        return this;
    }


    public MessageConsumer getResponseConsumer() {
        if (responseConsumer == null) {
            try {
                responseConsumer = getSession().createConsumer(getResponseDestination());
            } catch (JMSException e) {
                throw new IllegalStateException(e);
            }
        }
        return responseConsumer;
    }

    public JmsBuildUtils withResponseConsumer(final MessageConsumer responseConsumer) {
        this.responseConsumer = responseConsumer;
        return this;
    }


    public String getUserNameConnection() {
        return userNameConnection;
    }

    public JmsBuildUtils withUserNameConnection(String userNameConnection) {
        this.userNameConnection = userNameConnection;
        return this;
    }

    public String getPasswordConnection() {
        return passwordConnection;
    }

    public JmsBuildUtils withPasswordConnection(String passwordConnection) {
        this.passwordConnection = passwordConnection;
        return this;
    }


    public JmsBuildUtils withConnectionCreator(Function<ConnectionFactory, Connection> connectionCreator) {
        this.connectionCreator = connectionCreator;
        return this;
    }

    public int getAcknowledgeSessionMode() {
        return acknowledgeSessionMode;
    }

    public JmsBuildUtils withAcknowledgeSessionMode(final int acknowledgeMode) {
        this.acknowledgeSessionMode = acknowledgeMode;
        return this;
    }

    public boolean isTransactionalSession() {
        return transactionalSession;
    }

    public JmsBuildUtils withTransactionalSession(final boolean transactionalSession) {
        this.transactionalSession = transactionalSession;
        return this;
    }


    public Context getContext() {
        if (context == null) {
            try {
                context = new InitialContext(getJndiProperties());
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }
        return context;
    }

    public JmsBuildUtils withContext(final Context context) {
        this.context = context;
        return this;
    }

    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            try {
                connectionFactory = (ConnectionFactory) getContext().lookup(getConnectionFactoryName());
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }
        return connectionFactory;
    }

    public JmsBuildUtils withConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public Destination getDestination() {
        if (destination == null) {

            try {
                if (!ibmMQ) {
                    destination = (Destination) getContext().lookup(getDestinationName());
                } else
                    destination = getSessionCreator().apply(getConnection()).createQueue(getDestinationName());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return destination;
    }

    public JmsBuildUtils withDestination(Destination destination) {
        this.destination = destination;
        return this;
    }

    public Session getSession() {
        if (session == null) {
            session = getSessionCreator().apply(getConnection());
        }
        return session;
    }

    public JmsBuildUtils withSession(Session session) {
        this.session = session;
        return this;
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                if (getUserNameConnection() == null || getPasswordConnection() == null) {

                    connection = getConnectionFactory().createConnection();

                } else {
                    connection = getConnectionFactory().createConnection(getUserNameConnection(), getPasswordConnection());
                }
                connection.start();
            } catch (JMSException e) {
                throw new IllegalStateException(e);
            }
        }
        return connection;
    }

    public JmsBuildUtils withConnection(final Connection connection) {
        this.connection = connection;
        return this;
    }

    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }

    public JmsBuildUtils withConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
        return this;
    }

    public String getResponseDestinationName() {
        return responseDestinationName;
    }

    public JmsBuildUtils withResponseDestinationName(String replyDestinationName) {
        this.responseDestinationName = replyDestinationName;
        return this;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public JmsBuildUtils withDestinationName(String destinationName) {
        this.destinationName = destinationName;
        return this;
    }

    public Function<Connection, Session> getSessionCreator() {
        if (sessionCreator == null) {
            sessionCreator = new Function<Connection, Session>() {
                @Override
                public Session apply(Connection connection) {
                    try {
                        return connection.createSession(isTransactionalSession(), getAcknowledgeSessionMode());
                    } catch (JMSException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
        }
        return sessionCreator;
    }

    public JmsBuildUtils withSessionCreator(final Function<Connection, Session> sessionCreator) {
        this.sessionCreator = sessionCreator;
        return this;
    }


    public JmsBuildUtils useIBMMQ() {
        ibmMQ = true;


        final String queueManager = getIbmMQQueueManager();
        final String channel = getIbmMQChannel();


        jndiProperties.clear();
        jndiProperties.put("java.naming.factory.initial", System.getenv().getOrDefault("NATS_BRIDGE_JMS_NAMING_FACTORY", "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"));
        jndiProperties.put("nats.ibm.mq.host", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_HOST", "tcp://localhost:1414"));
        jndiProperties.put("nats.ibm.mq.channel", channel);
        jndiProperties.put("nats.ibm.mq.queueManager", queueManager);

        if (isRequestReply()) {
            final String queueModelName = getIbmMQQueueModelName();
            final String queueModelPrefix = getIbmMQQueueModelPrefix();
            jndiProperties.put("nats.ibm.mq.queueModelName", queueModelName);
            jndiProperties.put("nats.ibm.mq.queueModelPrefix", queueModelPrefix);
        }

        return this;
    }

    public Hashtable<String, Object> getJndiProperties() {
        if (jndiProperties.size() == 0) {
            jndiProperties.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
            jndiProperties.put("connectionFactory.ConnectionFactory",  "tcp://localhost:61616");
            jndiProperties.put("queue.queue/testQueue", "queue.queue/testQueue=testQueue");
        }
        return jndiProperties;
    }

    public JmsBuildUtils setJndiProperties(Hashtable<String, Object> jndiProperties) {
        this.jndiProperties = jndiProperties;
        return this;
    }
}
