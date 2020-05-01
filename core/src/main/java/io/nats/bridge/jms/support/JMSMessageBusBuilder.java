// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.nats.bridge.jms.support;

import io.nats.bridge.MessageBus;
import io.nats.bridge.TimeSource;
import io.nats.bridge.jms.JMSMessageBus;
import io.nats.bridge.jms.JMSMessageBusException;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsDisplay;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.Output;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import io.nats.bridge.nats.support.NatsBuilderException;
import io.nats.bridge.support.MessageBusBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.FunctionWithException;
import io.nats.bridge.util.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Function;
import java.util.function.Supplier;

public class JMSMessageBusBuilder implements MessageBusBuilder {

    private final ExceptionHandler exceptionHandler = new ExceptionHandler(LoggerFactory.getLogger(JMSMessageBusBuilder.class));
    private Logger jmsBusLogger;
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
    private TimeSource timeSource;
    private Destination responseDestination;
    private MessageConsumer responseConsumer;
    private Supplier<MessageProducer> producerSupplier;
    private Supplier<MessageConsumer> consumerSupplier;
    private Metrics metrics;
    private MetricsProcessor metricsProcessor;
    private ExceptionHandler tryHandler;
    private FunctionWithException<Message, io.nats.bridge.messages.Message> jmsMessageConverter;
    private FunctionWithException<io.nats.bridge.messages.Message, javax.jms.Message> bridgeMessageConverter;
    private java.util.Queue<JMSReply> jmsReplyQueue;
    private boolean copyHeaders = false;
    private Hashtable<String, Object> jndiProperties = new Hashtable<>();
    private String name = "jms-no-name";
    private boolean ibmMQ = false;
    private String responseDestinationName = "TEMP_QUEUE";
    private boolean source = false;


    public static JMSMessageBusBuilder builder() {
        return new JMSMessageBusBuilder();
    }

    public String getName() {
        return name;
    }

    public JMSMessageBusBuilder asSource() {

        source = true;
        return this;
    }

    public JMSMessageBusBuilder withName(String name) {
        if (name == null) {
            throw new NatsBuilderException("Name must be set");
        }
        this.name = name;
        return this;
    }

    public FunctionWithException<io.nats.bridge.messages.Message, Message> getBridgeMessageConverter() {

        if (bridgeMessageConverter == null) {
            if (isCopyHeaders()) {
                bridgeMessageConverter = new ConvertBridgeMessageToJmsMessageWithHeaders(getSession());
            } else {
                bridgeMessageConverter = new ConvertBridgeMessageToJmsMessage(getSession());
            }
        }
        return bridgeMessageConverter;
    }

    public JMSMessageBusBuilder withBridgeMessageConverter(final FunctionWithException<io.nats.bridge.messages.Message, Message> bridgeMessageConverter) {
        this.bridgeMessageConverter = bridgeMessageConverter;
        return this;
    }

    public boolean isCopyHeaders() {
        return copyHeaders;
    }

    public JMSMessageBusBuilder turnOnCopyHeaders() {
        return withCopyHeaders(true);
    }

    public JMSMessageBusBuilder withCopyHeaders(boolean copyHeaders) {
        this.copyHeaders = copyHeaders;
        return this;
    }

    public Queue<JMSReply> getJmsReplyQueue() {
        if (jmsReplyQueue == null) {
            jmsReplyQueue = new LinkedTransferQueue<>();
        }
        return jmsReplyQueue;
    }

    public JMSMessageBusBuilder withJmsReplyQueue(Queue<JMSReply> jmsReplyQueue) {
        this.jmsReplyQueue = jmsReplyQueue;
        return this;
    }

    public FunctionWithException<Message, io.nats.bridge.messages.Message> getJmsMessageConverter() {
        if (jmsMessageConverter == null) {
            if (!isCopyHeaders()) {
                jmsMessageConverter = new ConvertJmsMessageToBridgeMessage(getTryHandler(), getTimeSource(), getJmsReplyQueue(), "" + getName() + "_CONVERT_JMS_MESSAGE");
            } else {
                jmsMessageConverter = new ConvertJmsMessageToBridgeMessageWithHeaders(getTryHandler(), getTimeSource(), getJmsReplyQueue(), "" + getName() + "_CONVERT_JMS_MESSAGE_W_HEADERS");
            }
        }
        return jmsMessageConverter;
    }

    public JMSMessageBusBuilder withJmsMessageConverter(final FunctionWithException<Message, io.nats.bridge.messages.Message> messageConverter) {
        this.jmsMessageConverter = messageConverter;
        return this;
    }

    public JMSMessageBusBuilder withJndiProperty(String name, String value) {
        this.getJndiProperties().put(name, value);
        return this;
    }

    public JMSMessageBusBuilder withJndiProperties(Map<String, String> props) {
        this.getJndiProperties().putAll(props);
        return this;
    }

    public ExceptionHandler getTryHandler() {
        if (tryHandler == null) {
            tryHandler = new ExceptionHandler(getJmsBusLogger());
        }
        return tryHandler;
    }

    public JMSMessageBusBuilder withTryHandler(ExceptionHandler tryHandler) {
        this.tryHandler = tryHandler;
        return this;
    }

    public Logger getJmsBusLogger() {
        if (jmsBusLogger == null) {
            jmsBusLogger = LoggerFactory.getLogger(JMSMessageBus.class.toString() + "_" + getName());
        }
        return jmsBusLogger;
    }

    public JMSMessageBusBuilder withJmsBusLogger(Logger jmsBusLogger) {
        this.jmsBusLogger = jmsBusLogger;
        return this;
    }

    public MetricsProcessor getMetricsProcessor() {
        if (metricsProcessor == null) {
            metricsProcessor = new MetricsDisplay(new Output() {
            }, getMetrics(), 10, Duration.ofSeconds(10), System::currentTimeMillis, name);
        }
        return metricsProcessor;
    }

    public JMSMessageBusBuilder withMetricsProcessor(MetricsProcessor metricsProcessor) {
        this.metricsProcessor = metricsProcessor;
        return this;
    }

    public Supplier<MessageProducer> getProducerSupplier() {
        if (producerSupplier == null) {
            producerSupplier = () -> getTryHandler().tryReturnOrRethrow(() -> getSession().createProducer(getDestination()),
                    e -> new JMSMessageBusException("unable to create producer", e));
        }
        return producerSupplier;
    }

    public void withProducerSupplier(final Supplier<MessageProducer> producerSupplier) {
        this.producerSupplier = producerSupplier;
    }


    public Supplier<MessageConsumer> getConsumerSupplier() {

        if (consumerSupplier == null) {
            consumerSupplier = () -> getTryHandler().tryReturnOrRethrow(() -> getSession().createConsumer(getDestination()),
                    e -> new JMSMessageBusException("unable to create consumer", e));
        }
        return consumerSupplier;
    }

    public void withConsumerSupplier(final Supplier<MessageConsumer> consumerSupplier) {
        this.consumerSupplier = consumerSupplier;
    }

    public Metrics getMetrics() {
        if (metrics == null) {
            metrics = new SimpleMetrics(System::currentTimeMillis);
        }
        return metrics;
    }

    public JMSMessageBusBuilder withMetrics(final Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public Destination getResponseDestination() {
        if (responseDestination == null) {
            responseDestination = exceptionHandler.tryReturnOrRethrow(() -> {

                        if ((getResponseDestinationName().equals("TEMP_QUEUE"))) {
                            System.out.println("Creating temp queue ");
                            return getSession().createTemporaryQueue();
                        } else {
                            System.out.println("Creating response queue " + getResponseDestinationName());
                            return getSession().createQueue(getResponseDestinationName());
                        }

                    },
                    e -> new JMSMessageBusBuilderException("Unable to create JMS response queue " + getUserNameConnection(), e));
        }
        return responseDestination;
    }

    public JMSMessageBusBuilder withResponseDestination(Destination responseDestination) {
        this.responseDestination = responseDestination;
        return this;
    }

    public MessageConsumer getResponseConsumer() {
        if (responseConsumer == null) {
            responseConsumer = exceptionHandler.tryReturnOrRethrow(() -> getSession().createConsumer(getResponseDestination()),
                    e -> new JMSMessageBusBuilderException("Unable to create JMS response consumer " + getUserNameConnection(), e));
        }
        return responseConsumer;
    }

    public JMSMessageBusBuilder withResponseConsumer(final MessageConsumer responseConsumer) {
        this.responseConsumer = responseConsumer;
        return this;
    }

    public TimeSource getTimeSource() {
        if (timeSource == null) {
            timeSource = System::currentTimeMillis;
        }
        return timeSource;
    }

    public JMSMessageBusBuilder withTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
        return this;
    }

    public String getUserNameConnection() {
        if (userNameConnection == null || userNameConnection.trim().isEmpty()) {
            userNameConnection = System.getenv("NATS_BRIDGE_JMS_USER");
        }

        return userNameConnection;
    }

    public JMSMessageBusBuilder withUserNameConnection(String userNameConnection) {
        this.userNameConnection = userNameConnection;
        return this;
    }

    public String getPasswordConnection() {
        if (passwordConnection == null) {
            passwordConnection = System.getenv("NATS_BRIDGE_JMS_PWD");
        }
        return passwordConnection;
    }

    public JMSMessageBusBuilder withPasswordConnection(String passwordConnection) {
        this.passwordConnection = passwordConnection;
        return this;
    }

    public Function<ConnectionFactory, Connection> getConnectionCreator() {
        if (connectionCreator == null) {
            connectionCreator = connectionFactory -> getTryHandler().tryReturnOrRethrow(() -> {
                Connection connection;
                if (getUserNameConnection() == null || getPasswordConnection() == null) {
                    connection = connectionFactory.createConnection();
                } else {
                    connection = connectionFactory.createConnection(getUserNameConnection(), getPasswordConnection());
                }
                connection.start();
                return connection;
            }, e -> new JMSMessageBusBuilderException("Unable to create JMS connection " + getUserNameConnection(), e));
        }
        return connectionCreator;
    }

    public JMSMessageBusBuilder withConnectionCreator(Function<ConnectionFactory, Connection> connectionCreator) {
        this.connectionCreator = connectionCreator;
        return this;
    }

    public int getAcknowledgeSessionMode() {
        return acknowledgeSessionMode;
    }

    public JMSMessageBusBuilder withAcknowledgeSessionMode(final int acknowledgeMode) {
        this.acknowledgeSessionMode = acknowledgeMode;
        return this;
    }

    public boolean isTransactionalSession() {
        return transactionalSession;
    }

    public JMSMessageBusBuilder withTransactionalSession(final boolean transactionalSession) {
        this.transactionalSession = transactionalSession;
        return this;
    }


    public Context getContext() {
        if (context == null) {


            context = exceptionHandler.tryReturnOrRethrow((SupplierWithException<Context>) () -> new InitialContext(getJndiProperties()),
                    (e) -> new JMSMessageBusBuilderException("Unable to create JNDI context", e));
        }
        return context;
    }

    public JMSMessageBusBuilder withContext(final Context context) {
        this.context = context;
        return this;
    }

    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            connectionFactory = exceptionHandler.tryReturnOrRethrow(() -> (ConnectionFactory) getContext().lookup(getConnectionFactoryName()),
                    e -> new JMSMessageBusBuilderException("Unable to lookup connection factory " + getConnectionFactoryName(), e));
        }
        return connectionFactory;
    }

    public JMSMessageBusBuilder withConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public Destination getDestination() {
        if (destination == null) {
            if (!ibmMQ)
                destination = exceptionHandler.tryReturnOrRethrow(() -> (Destination) getContext().lookup(getDestinationName()),
                        e -> new JMSMessageBusBuilderException("Unable to lookup destination " + getDestinationName(), e));
            else
                destination = exceptionHandler.tryReturnOrRethrow(() ->
                                getSessionCreator().apply(getConnection()).createQueue(getDestinationName()),
                        e -> new JMSMessageBusBuilderException("Unable to lookup destination " + getDestinationName(), e));
        }
        return destination;
    }

    public JMSMessageBusBuilder withDestination(Destination destination) {
        this.destination = destination;
        return this;
    }

    public Session getSession() {
        if (session == null) {
            session = getSessionCreator().apply(getConnection());
        }
        return session;
    }

    public JMSMessageBusBuilder withSession(Session session) {
        this.session = session;
        return this;
    }

    public Connection getConnection() {
        if (connection == null) {
            connection = getConnectionCreator().apply(getConnectionFactory());
        }
        return connection;
    }

    public JMSMessageBusBuilder withConnection(final Connection connection) {
        this.connection = connection;
        return this;
    }

    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }

    public JMSMessageBusBuilder withConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
        return this;
    }

    public String getResponseDestinationName() {
        return responseDestinationName;
    }

    public JMSMessageBusBuilder withResponseDestinationName(String replyDestinationName) {
        this.responseDestinationName = replyDestinationName;
        return this;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public JMSMessageBusBuilder withDestinationName(String destinationName) {
        this.destinationName = destinationName;
        return this;
    }

    public Function<Connection, Session> getSessionCreator() {
        if (sessionCreator == null) {
            sessionCreator = connection -> getTryHandler().tryReturnOrRethrow(() ->
                            connection.createSession(isTransactionalSession(), getAcknowledgeSessionMode()),
                    e -> new JMSMessageBusBuilderException("Unable to create session", e));
        }
        return sessionCreator;
    }

    public JMSMessageBusBuilder withSessionCreator(final Function<Connection, Session> sessionCreator) {
        this.sessionCreator = sessionCreator;
        return this;
    }

    public MessageBus build() {

        if (getConnectionFactory() != null && getConnectionFactory().getClass().getPackage().getName().contains("io.nats.bridge.ibmmq")) {
            ibmMQ = true;
        }

        final Connection connection = getConnection();
        final Session session = getSession();
        final Destination destination = getDestination();
        return new JMSMessageBus(getName(), destination, session, connection,
                getResponseDestination(),
                source ? null : getResponseConsumer(),
                getTimeSource(), getMetrics(), getProducerSupplier(), getConsumerSupplier(),
                getMetricsProcessor(), getTryHandler(), getJmsBusLogger(), getJmsReplyQueue(), getJmsMessageConverter(),
                getBridgeMessageConverter(), getDestinationName());
    }


    public JMSMessageBusBuilder useIBMMQ(final Hashtable<String, Object> jndiProperties) {

        getJmsBusLogger().info("CLEARING JNDI PROPERTIES");
        ibmMQ = true;
        this.jndiProperties.clear();
        jndiProperties.put("java.naming.factory.initial", System.getenv().getOrDefault("NATS_BRIDGE_JMS_NAMING_FACTORY", "io.nats.bridge.ibmmq.IbmMqInitialContextFactory"));
        this.jndiProperties.putAll(jndiProperties);
        return this;
    }

    public JMSMessageBusBuilder useIBMMQ() {
        getJmsBusLogger().info("CLEARING JNDI PROPERTIES");
        ibmMQ = true;
        jndiProperties.clear();
        jndiProperties.put("java.naming.factory.initial", System.getenv().getOrDefault("NATS_BRIDGE_JMS_NAMING_FACTORY", "io.nats.bridge.ibmmq.IbmMqInitialContextFactory"));
        jndiProperties.put("nats.ibm.mq.host", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_HOST", "tcp://localhost:1414"));
        jndiProperties.put("nats.ibm.mq.channel", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_CHANNEL", "DEV.APP.SVRCONN"));
        jndiProperties.put("nats.ibm.mq.queueManager", System.getenv().getOrDefault("NATS_BRIDGE_IBM_MQ_QUEUE_MANAGER", "QM1"));
        return this;
    }

    public Hashtable<String, Object> getJndiProperties() {
        if (jndiProperties.size() == 0) {
            jndiProperties.put("java.naming.factory.initial", System.getenv().getOrDefault("NATS_BRIDGE_JMS_NAMING_FACTORY", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"));
            jndiProperties.put("connectionFactory.ConnectionFactory", System.getenv().getOrDefault("NATS_BRIDGE_JMS_CONNECTION_FACTORY", "tcp://localhost:61616"));
            jndiProperties.put("queue.queue/testQueue", System.getenv().getOrDefault("NATS_BRIDGE_JMS_QUEUE", "queue.queue/testQueue=testQueue"));
        }
        return jndiProperties;
    }

    public JMSMessageBusBuilder setJndiProperties(Hashtable<String, Object> jndiProperties) {
        this.jndiProperties = jndiProperties;
        return this;
    }
}
