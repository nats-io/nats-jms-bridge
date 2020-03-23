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
import io.nats.bridge.metrics.implementation.MetricsImpl;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class JMSMessageBusBuilder {

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
            producerSupplier = () -> {
                try {
                    return  getSession().createProducer(getDestination());
                } catch (Exception ex) {
                    throw new JMSMessageBusException("unable to create producer", ex);
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
            consumerSupplier =  () -> {
                try {
                    return getSession().createConsumer(getDestination());
                } catch (Exception ex) {
                    throw new JMSMessageBusException("unable to create consumer", ex);
                }
            };
        }
        return consumerSupplier;
    }

    public void withConsumerSupplier(final Supplier<MessageConsumer> consumerSupplier) {
        this.consumerSupplier = consumerSupplier;
    }

    public Metrics getMetrics() {
        if (metrics == null) {
            metrics = new MetricsImpl(System::currentTimeMillis);
        }
        return metrics;
    }

    public JMSMessageBusBuilder withMetrics(final Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public Destination getResponseDestination() {
        if (responseDestination == null) {
            try {
                responseDestination = getSession().createTemporaryQueue();
            } catch (JMSException ex) {
                throw new JMSMessageBusBuilderException("Unable to create JMS response queue " + getUserNameConnection(), ex);
            }
        }
        return responseDestination;
    }

    public JMSMessageBusBuilder withResponseDestination(Destination responseDestination) {
        this.responseDestination = responseDestination;
        return this;
    }

    public MessageConsumer getResponseConsumer() {
        if (responseConsumer == null) {
            try {
                responseConsumer = getSession().createConsumer(getResponseDestination());
            } catch (JMSException ex) {
                throw new JMSMessageBusBuilderException("Unable to create JMS response consumer " + getUserNameConnection(), ex);
            }
        }
        return responseConsumer;
    }

    public JMSMessageBusBuilder withResponseConsumer(MessageConsumer responseConsumer) {
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
            connectionCreator = connectionFactory -> {
                try {
                    Connection connection;
                    if (getUserNameConnection() == null || getPasswordConnection() == null) {
                        connection = connectionFactory.createConnection();
                    } else {
                        connection = connectionFactory.createConnection(getUserNameConnection(), getPasswordConnection());
                    }
                    connection.start();
                    return connection;
                } catch (Exception ex) {
                    throw new JMSMessageBusBuilderException("Unable to create JMS connection " + getUserNameConnection(), ex);
                }
            };

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
            try {
                context = new InitialContext();
            } catch (NamingException e) {
                throw new JMSMessageBusBuilderException("Unable to create JNDI context", e);
            }
        }
        return context;
    }

    public JMSMessageBusBuilder withContext(final Context context) {
        this.context = context;
        return this;
    }

    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            try {
                connectionFactory = (ConnectionFactory) getContext().lookup(getConnectionFactoryName());
            } catch (Exception e) {
                throw new JMSMessageBusBuilderException("Unable to lookup connection factory " + getConnectionFactoryName(), e);
            }
        }
        return connectionFactory;
    }

    public JMSMessageBusBuilder withConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public Destination getDestination() {
        if (destination == null) {
            try {
                destination = (Destination) getContext().lookup(getDestinationName());
            } catch (Exception e) {
                throw new JMSMessageBusBuilderException("Unable to lookup destination " + getDestinationName(), e);
            }
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
            sessionCreator = connection -> {
                try {
                    return connection.createSession(isTransactionalSession(), getAcknowledgeSessionMode());
                } catch (Exception ex) {
                    throw new JMSMessageBusBuilderException("Unable to create session", ex);
                }
            };
        }
        return sessionCreator;
    }

    public JMSMessageBusBuilder withSessionCreator(Function<Connection, Session> sessionCreator) {
        this.sessionCreator = sessionCreator;
        return this;
    }

    public MessageBus build() {
        return new JMSMessageBus(getDestination(), getSession(), getConnection(), getResponseDestination(),
                getResponseConsumer(), getTimeSource(), getMetrics(), getProducerSupplier(), getConsumerSupplier(), getMetricsProcessor());
    }



}
