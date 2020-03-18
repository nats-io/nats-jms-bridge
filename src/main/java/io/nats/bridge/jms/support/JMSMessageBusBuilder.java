package io.nats.bridge.jms.support;


import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.JMSMessageBus;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.function.Function;

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

    public void setConnectionCreator(Function<ConnectionFactory, Connection> connectionCreator) {
        this.connectionCreator = connectionCreator;
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
        return new JMSMessageBus(getDestination(), getSession(), getConnection());
    }

}
