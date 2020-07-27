package io.nats.bridge.jms;

import io.nats.bridge.util.ExceptionHandler;

import javax.jms.*;
import java.util.function.Supplier;

public class JmsContext {



    private final Destination destination;
    private final Session session;
    private final Connection connection;
    private final Destination responseDestination;
    private final MessageConsumer responseConsumer;
    private final Supplier<MessageProducer> producerSupplier;
    private final Supplier<MessageConsumer>  consumerSupplier;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private final ExceptionHandler tryHandler;


    public JmsContext(final Destination destination,
                      final Session session,
                      final Connection connection,
                      final Destination responseDestination,
                      final MessageConsumer responseConsumer,
                      final Supplier<MessageProducer> producer,
                      final Supplier<MessageConsumer> consumer, ExceptionHandler tryHandler) {
        this.destination = destination;
        this.session = session;
        this.connection = connection;
        this.responseDestination = responseDestination;
        this.responseConsumer = responseConsumer;
        this.producerSupplier = producer;
        this.consumerSupplier = consumer;
        this.tryHandler = tryHandler;
    }

    public Destination getDestination() {
        return destination;
    }

    public Session getSession() {
        return session;
    }

    public Connection getConnection() {
        return connection;
    }

    public Destination getResponseDestination() {
        return responseDestination;
    }

    public MessageConsumer getResponseConsumer() {
        return responseConsumer;
    }


    public MessageProducer producer() {
        if (producer == null) {
            producer = producerSupplier.get();
        }
        return producer;
    }

    public MessageConsumer consumer() {
        if (consumer == null) {
            consumer = consumerSupplier.get();
        }
        return consumer;
    }

    public void close() {
        tryHandler.tryWithRethrow(connection::close, e -> new JMSMessageBusException("Error closing connection", e));
    }
}
