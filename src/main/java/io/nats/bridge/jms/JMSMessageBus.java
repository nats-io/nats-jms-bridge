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
import io.nats.bridge.jms.support.JMSReply;
import io.nats.bridge.jms.support.JMSRequestResponse;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsProcessor;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class JMSMessageBus implements MessageBus {

    private final Destination destination;
    private final Session session;
    private final Connection connection;

    private final Destination responseDestination;
    private final MessageConsumer responseConsumer;
    private final TimeSource timeSource;
    private final Map<String, JMSRequestResponse> requestResponseMap = new HashMap<>();
    private final Counter countRequestResponseErrors;


    private MessageProducer producer;
    private MessageConsumer consumer;

    private final Metrics metrics;


    private final Counter countPublish;
    private final Counter countPublishErrors;
    private final Counter countRequest;
    private final Counter countRequestErrors;
    private final Counter countRequestResponses;
    private final Counter countRequestResponsesMissing;

    private java.util.Queue<JMSReply> jmsReplyQueue = new LinkedTransferQueue<>();

    private final Supplier<MessageProducer> producerSupplier;
    private final Supplier<MessageConsumer> consumerSupplier;

    private final MetricsProcessor metricsProcessor;

    public JMSMessageBus(final Destination destination, final Session session,
                         final Connection connection, final Destination responseDestination,
                         final MessageConsumer responseConsumer, final TimeSource timeSource, final Metrics metrics,
                         final Supplier<MessageProducer> producerSupplier, final Supplier<MessageConsumer> consumerSupplier, MetricsProcessor metricsProcessor) {
        this.destination = destination;
        this.session = session;
        this.connection = connection;
        this.responseDestination = responseDestination;
        this.responseConsumer = responseConsumer;
        //TODO setup exception listener for JMS Connection
        this.timeSource = timeSource;


        this.metrics = metrics;
        countPublish = metrics.createCounter("publish-count");
        countPublishErrors = metrics.createCounter("publish-count-errors");
        countRequest = metrics.createCounter("request-count");
        countRequestErrors = metrics.createCounter("request-count-errors");
        countRequestResponses = metrics.createCounter("request-response-count");
        countRequestResponseErrors = metrics.createCounter("request-response-count-errors");
        countRequestResponsesMissing = metrics.createCounter("request-response-missing-count");


        this.producerSupplier = producerSupplier;

        this.consumerSupplier = consumerSupplier;
        this.metricsProcessor = metricsProcessor;
    }

    private MessageProducer producer() {
        if (producer == null) {
            producer = producerSupplier.get();
        }
        return producer;
    }

    private MessageConsumer consumer() {
        if (consumer == null) {
            consumer = consumerSupplier.get();
        }
        return consumer;
    }

    @Override
    public void publish(Message message) {
        try {
            producer().send(convertToJMSMessage(message));
            this.countPublish.increment();
        } catch (Exception ex) {
            this.countPublishErrors.increment();
            throw new JMSMessageBusException("Unable to send the message to the producer", ex);
        }
    }


    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {
        //TODO to get this to be more generic as part of builder pass a createDestination Function<Session, Destination> that calls session.createTemporaryQueue() or session.createTemporaryTopic()
        final javax.jms.Message jmsMessage = convertToJMSMessage(message);


        try {

            final String correlationID = UUID.randomUUID().toString();
            jmsMessage.setJMSReplyTo(responseDestination);
            jmsMessage.setJMSCorrelationID(correlationID);
            producer().send(jmsMessage);
            if (message instanceof StringMessage) {
                System.out.println("REQUEST BODY " + ((StringMessage) message).getBody());
            }
            System.out.printf("CORRELATION ID: %s %s\n", correlationID, responseDestination.toString());
            requestResponseMap.put(correlationID, new JMSRequestResponse(correlationID, replyCallback, timeSource.getTime()));
            countRequest.increment();

        } catch (final JMSException e) {
            countRequestErrors.increment();
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


    private void enqueueReply(final StringMessage reply, final String correlationID, final Destination jmsReplyTo) {

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
    // There is another class that deals with a collection of JMSMessageBus called a bridge
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


    /**
     * This gets called by bridge to process outstanding responses.
     * If the client is Nats and the Server is JMS then there will be messages from the `responseConsumer`.
     */
    private void processResponses() {
        javax.jms.Message message;
        try {
            do {
                message = responseConsumer.receiveNoWait();
                if (message != null) {
                    final String correlationID = message.getJMSCorrelationID();

                    System.out.printf("Process JMS Message Consumer %s %s \n", correlationID, ((TextMessage) message).getText());
                    Optional<JMSRequestResponse> jmsRequestResponse = Optional.ofNullable(requestResponseMap.get(correlationID));


                    final javax.jms.Message msg = message;

                    jmsRequestResponse.ifPresent(requestResponse -> {
                        requestResponse.getReplyCallback().accept(convertToBusMessage(msg));
                        countRequestResponses.increment();
                    });

                    if (!jmsRequestResponse.isPresent()) {
                        countRequestResponsesMissing.increment();
                    }
                }

            }
            while (message != null);

        } catch (JMSException ex) {
            countRequestResponseErrors.increment();
            ex.printStackTrace();
        }

    }

    @Override
    public void process() {
        processResponses();
        processReplies();

        metricsProcessor.process();
    }

    /**
     * This gets called to process replies.
     * If the client is JMS and the Server is Nats then there will be replies to process.
     */
    private void processReplies() {

        try {
            JMSReply reply = null;
            do {
                reply = jmsReplyQueue.poll();
                if (reply != null) {

                    final String messageBody = reply.getReply().getBody();
                    final String correlationId = reply.getCorrelationID();
                    final MessageProducer replyProducer = session.createProducer(reply.getJmsReplyTo());
                    final TextMessage jmsReplyMessage = session.createTextMessage(messageBody);

                    System.out.printf("Reply handler - %s %s %s\n", messageBody, correlationId, replyProducer.getDestination().toString());
                    jmsReplyMessage.setJMSCorrelationID(correlationId);

                    replyProducer.send(jmsReplyMessage);
                    // TODO close these BOTH nicer.
                    replyProducer.close();
                }
            } while (reply != null);


        } catch (
                JMSException e) {
            throw new JMSMessageBusException("error processing JMS receive queue", e);
        }
    }
}
