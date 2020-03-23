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
package io.nats.bridge.jms;

import io.nats.bridge.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.StringMessage;
import io.nats.bridge.TimeSource;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;


public class JMSMessageBus implements MessageBus {

    private final Destination destination;
    private final Session session;
    private final Connection connection;

    private final Destination responseDestination;
    private final MessageConsumer responseConsumer;
    private final TimeSource timeSource;
    private final Map<String, JMSRequestResponse> requestResponseMap = new HashMap<>();


    private MessageProducer producer;
    private MessageConsumer consumer;


    public JMSMessageBus(final Destination destination, final Session session, final Connection connection, final TimeSource timeSource) {
        this.destination = destination;
        this.session = session;
        this.connection = connection;
        //TODO setup exception listener for JMS Connection
        this.timeSource = timeSource;
        try {
            this.responseDestination = session.createTemporaryQueue();
            this.responseConsumer = session.createConsumer(responseDestination);
        } catch (JMSException e) {
            throw new JMSMessageBusException("unable to create response destination", e);
        }

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

    class JMSRequestResponse {
        private final String jmsCorrelationID;
        private final Consumer<Message> replyCallback;
        private final long sentTime;

        public String getJmsCorrelationID() {
            return jmsCorrelationID;
        }

        public Consumer<Message> getReplyCallback() {
            return replyCallback;
        }

        public long getSentTime() {
            return sentTime;
        }

        public JMSRequestResponse(final String jmsCorrelationID, final Consumer<Message> replyCallback, final long sentTime) {
            this.jmsCorrelationID = jmsCorrelationID;
            this.replyCallback = replyCallback;
            this.sentTime = sentTime;
        }
    }


    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {
        //TODO to get this to be more generic as part of builder pass a createDestination Function<Session, Destination> that calls session.createTemporaryQueue() or session.createTemporaryTopic()
        final javax.jms.Message jmsMessage = convertToJMSMessage(message);


        if (message instanceof StringMessage) {
            System.out.println("REQUEST BODY " + ((StringMessage) message).getBody());
        }

        try {

            final String correlationID = UUID.randomUUID().toString();
            jmsMessage.setJMSReplyTo(responseDestination);
            jmsMessage.setJMSCorrelationID(correlationID);
            producer().send(jmsMessage);


            System.out.println("CORRELATION ID: " + correlationID);
            requestResponseMap.put(correlationID, new JMSRequestResponse(correlationID, replyCallback, timeSource.getTime()));

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


    class JMSReply {
        private final StringMessage reply;
        private final String correlationID;
        private final Destination jmsReplyTo;

        public StringMessage getReply() {
            return reply;
        }

        public String getCorrelationID() {
            return correlationID;
        }

        public Destination getJmsReplyTo() {
            return jmsReplyTo;
        }

        JMSReply(final StringMessage reply, final String correlationID, final Destination jmsReplyTo) {
            this.reply = reply;
            this.correlationID = correlationID;
            this.jmsReplyTo = jmsReplyTo;
        }
    }

    private java.util.Queue<JMSReply> jmsReplyQueue = new LinkedTransferQueue<>();

    private void enqueueReply(final StringMessage reply, final String correlationID, final Destination jmsReplyTo) throws JMSException {

        jmsReplyQueue.add(new JMSReply(reply, correlationID, jmsReplyTo));


    }

    //TODO pass this a Function<JMSMessage, Message> as part of the builder
    private Message convertToBusMessage(final javax.jms.Message jmsMessage) {
        if (jmsMessage instanceof TextMessage) {
            try {
                final Destination jmsReplyTo = jmsMessage.getJMSReplyTo();
                if (jmsReplyTo != null) {
                    return new StringMessage(((TextMessage) jmsMessage).getText()) {
                        @Override
                        public void reply(final Message reply) {
                            final StringMessage stringMessage = (StringMessage) reply;
                            try {
                                enqueueReply(stringMessage, jmsMessage.getJMSCorrelationID(), jmsReplyTo);
                            } catch (Exception ex) {
                                throw new JMSMessageBusException("Unable to send to JMS reply", ex);
                            }
                        }
                    };
                } else {
                    return new StringMessage(((TextMessage) jmsMessage).getText());
                }
            } catch (Exception ex) {
                throw new JMSMessageBusException("Unable to create JMS text message", ex);
            }
        } else if (jmsMessage == null) {
            return null;
        } else {
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

    @Override
    public void process() {


        javax.jms.Message message;
        try {
            do {
                message = responseConsumer.receiveNoWait();

                if (message != null) {
                    final String correlationID = message.getJMSCorrelationID();

                    System.out.printf("Process JMS Message Consumer %s %s \n", correlationID, ((TextMessage)message).getText());
                    Optional<JMSRequestResponse> jmsRequestResponse = Optional.ofNullable(requestResponseMap.get(correlationID));
                    final javax.jms.Message msg = message;


                    jmsRequestResponse.ifPresent(requestResponse -> {
                        requestResponse.getReplyCallback().accept(convertToBusMessage(msg));
                    });
                }

            }
            while (message != null);

            JMSReply reply = null;
            do {
                reply = jmsReplyQueue.poll();
                if (reply != null) {

                    final String messageBody = reply.getReply().getBody();
                    final String correlationId =  reply.getCorrelationID();
                    final MessageProducer replyProducer = session.createProducer(reply.getJmsReplyTo());
                    final TextMessage jmsReplyMessage = session.createTextMessage(messageBody);

                    System.out.printf("%s %s %s\n", messageBody, correlationId, replyProducer.getDestination().toString());
                    jmsReplyMessage.setJMSCorrelationID(correlationId);


                    replyProducer.send(jmsReplyMessage);
                    // TODO close these BOTH nicer.
                    replyProducer.close();
                }
            } while (reply != null);


        } catch (JMSException e) {
            throw new JMSMessageBusException("error processing JMS receive queue", e);
        }


    }
}
