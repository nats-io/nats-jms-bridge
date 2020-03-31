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
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class JMSMessageBusBuilder {

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
    private Metrics metrics;
    private Supplier<MessageProducer> producerSupplier;
    private Supplier<MessageConsumer> consumerSupplier;
    private MetricsProcessor metricsProcessor;
    private ExceptionHandler tryHandler;


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
            jmsBusLogger = LoggerFactory.getLogger(JMSMessageBus.class);
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
            }, getMetrics(), 10, Duration.ofSeconds(10), System::currentTimeMillis);
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
            responseDestination = exceptionHandler.tryReturnOrRethrow(() -> responseDestination = getSession().createTemporaryQueue(),
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
        return userNameConnection;
    }

    public JMSMessageBusBuilder withUserNameConnection(String userNameConnection) {
        this.userNameConnection = userNameConnection;
        return this;
    }

    public String getPasswordConnection() {
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
            context = exceptionHandler.tryReturnOrRethrow((SupplierWithException<Context>) InitialContext::new,
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
            destination = exceptionHandler.tryReturnOrRethrow(() -> (Destination) getContext().lookup(getDestinationName()),
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
        return new JMSMessageBus(getDestination(), getSession(), getConnection(), getResponseDestination(),
                getResponseConsumer(), getTimeSource(), getMetrics(), getProducerSupplier(), getConsumerSupplier(), getMetricsProcessor(), getTryHandler(), getJmsBusLogger());
    }


}
