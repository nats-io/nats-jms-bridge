package io.nats.bridge.jms;

import io.nats.bridge.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.StringMessage;

import javax.jms.*;
import java.util.Optional;
import java.util.function.Consumer;


public class JMSMessageBus implements MessageBus {

    private final Destination destination;
    private final Session session;
    private final Connection connection;

    //TODO setup exception listener for JMS Connection

    private MessageProducer producer;
    private MessageConsumer consumer;


    public JMSMessageBus(final Destination destination, final Session session, final Connection connection) {
        this.destination = destination;
        this.session = session;
        this.connection = connection;
    }

    private MessageProducer producer() {
        if (producer == null) {
            try {
                producer = session.createProducer(destination);
            } catch (Exception ex) {
                throw new JMSMessageBusException("unable to create producer", ex);
            }
        }
        return producer;
    }

    private MessageConsumer consumer() {
        if (consumer == null) {
            try {
                consumer = session.createConsumer(destination);
            } catch (Exception ex) {
                throw new JMSMessageBusException("unable to create consumer", ex);
            }
        }
        return consumer;
    }

    @Override
    public void publish(Message message) {
        try {
            producer().send(convertToJMSMessage(message));
        } catch (Exception ex) {
            throw new JMSMessageBusException("Unable to send the message to the producer", ex);
        }
    }

    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {
        javax.jms.Message jmsMessage = convertToJMSMessage(message);
        try {

            //TODO to get this to be more generic as part of builder pass a createDestination Function<Session, Destination> that calls session.createTemporaryQueue() or session.createTemporaryTopic()

            final Session session = connection.createSession(); //TODO we need to do this bc we are not using a reactor. goes faster when we don't create a session, but our log fills up.
            final Destination responseDestination = session.createTemporaryQueue();
            jmsMessage.setJMSReplyTo(responseDestination);
            producer().send(jmsMessage);
            final MessageConsumer consumer = session.createConsumer(responseDestination);
            // TODO set correlation id
            // TODO track messages that don't come back, like outstanding messages
            // You may want to put this into a linktransfer queue and poll it in a managed thread, so you can keep the stats in one place
            // Also, you will need to track how long it took and do a timeout exception and count if it took too long
            consumer.setMessageListener(replyMessage -> {
                replyCallback.accept(convertToBusMessage(replyMessage));



            });

//            /** Clean these up nicer. */
//            try {
//                consumer.close();
//                session.close();
//            } catch (JMSException e) {
//                e.printStackTrace();
//            }

        } catch (JMSException e) {
            throw new JMSMessageBusException("unable to send JMS request", e);
        }
    }

    //TODO pass this a Function<Message, JMSMessage> as part of the builder
    private javax.jms.Message convertToJMSMessage(final Message message) {
        if (message instanceof StringMessage) {
            try {
                return session.createTextMessage(((StringMessage) message).getBody());
            } catch (Exception ex) {
                throw new JMSMessageBusException("Unable to create JMS text message", ex);
            }
        } else {
            throw new JMSMessageBusException("Unexpected message type");
        }
    }

    //TODO pass this a Function<JMSMessage, Message> as part of the builder
    private Message convertToBusMessage(final javax.jms.Message message) {
        if (message instanceof TextMessage) {
            try {
                final Destination jmsReplyTo = message.getJMSReplyTo();
                if (jmsReplyTo != null) {
                    return new StringMessage(((TextMessage) message).getText()) {
                        @Override
                        public void reply(final Message reply) {
                            final StringMessage stringMessage = (StringMessage) reply;
                            try {
                                final Session session = connection.createSession();
                                final MessageProducer replyProducer = session.createProducer(jmsReplyTo);
                                final TextMessage message = session.createTextMessage(stringMessage.getBody());
                                replyProducer.send(message);
                                //TODO close these BOTH nicer.
                                replyProducer.close();
                                session.close();
                            } catch (Exception ex) {
                                throw new JMSMessageBusException("Unable to send to JMS reply", ex);
                            }
                        }
                    };
                } else {
                    return new StringMessage(((TextMessage) message).getText());
                }
            } catch (Exception ex) {
                throw new JMSMessageBusException("Unable to create JMS text message", ex);
            }
        } else if (message == null){
            return null;
        }else {
            throw new JMSMessageBusException("Unexpected message type");
        }
    }

    //TODO I imagine there being a bunch of these managed by one thread and then having a receive method that takes a Duration that gets called last.
    // There is a another class that deals with a collection of JMSMessageBus called a bridge
    @Override
    public Optional<Message> receive() {

        try {
            return Optional.ofNullable(convertToBusMessage(consumer().receiveNoWait()));
        } catch (JMSException e) {
            throw new JMSMessageBusException("Error receiving message", e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new JMSMessageBusException("Error closing connection", e);
        }
    }
}
