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

import io.nats.bridge.MessageBus;
import io.nats.bridge.TimeSource;
import io.nats.bridge.jms.support.JMSReply;
import io.nats.bridge.jms.support.JMSRequestResponse;
import io.nats.bridge.messages.Message;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.TimeTracker;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.FunctionWithException;
import org.slf4j.Logger;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.time.Duration;
import java.util.Queue;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class JMSMessageBus implements MessageBus {

    private final String name;
    private final TimeSource timeSource;
    private final Map<String, JMSRequestResponse> requestResponseMap = new HashMap<>();
    private final Metrics metrics;
    private final Counter countRequestResponseErrors;
    private final Counter countReceivedReply;
    private final Counter countReceivedReplyErrors;
    private final Counter countPublish;
    private final Counter countReceived;
    private final Counter countPublishErrors;
    private final Counter messageConvertErrors;
    private final Counter countRequest;
    private final Counter countRequestErrors;
    private final Counter countRequestResponses;
    private final Counter countRequestResponsesMissing;
    private final TimeTracker timerRequestResponse;
    private final TimeTracker timerReceiveReply;
    private final MetricsProcessor metricsProcessor;
    private final ExceptionHandler tryHandler;
    private final Logger logger;
    private final java.util.Queue<JMSReply> jmsReplyQueue;
    private final FunctionWithException<javax.jms.Message, Message> jmsMessageConverter;
    private  JmsContext jms;
    private final FunctionWithException<Message, javax.jms.Message> bridgeMessageConverter;
    private final Supplier<JmsContext> jmsSupplier;

    public JMSMessageBus(final String name,
                         final TimeSource timeSource,
                         final Metrics metrics,
                         final MetricsProcessor metricsProcessor,
                         final ExceptionHandler tryHandler,
                         final Logger logger,
                         final Queue<JMSReply> jmsReplyQueue,
                         final FunctionWithException<javax.jms.Message, Message> jmsMessageConverter,
                         final FunctionWithException<Message, javax.jms.Message> bridgeMessageConverter,
                         final String destinationName,
                         final Supplier<JmsContext> jmsSupplier) {
        this.name = name.toLowerCase().replace(".", "_").replace(" ", "_").replace("-", "_");
        this.timeSource = timeSource;
        this.tryHandler = tryHandler;


        this.metrics = metrics;
        this.jmsSupplier = jmsSupplier;



        final String[] tags = Metrics.tags("name", "name_" + this.name, "mb_type", "jms_mb", "dst", destinationName);

        countPublish = metrics.createCounter("publish_count", tags);
        countPublishErrors = metrics.createCounter("publish_count_errors", tags);
        countRequest = metrics.createCounter("request_count", tags);
        countRequestErrors = metrics.createCounter("request_count_errors", tags);
        countRequestResponses = metrics.createCounter("request_response_count", tags);
        countRequestResponseErrors = metrics.createCounter("request_response_count_errors", tags);
        countRequestResponsesMissing = metrics.createCounter("request_response_missing_count", tags);
        timerRequestResponse = metrics.createTimeTracker("request_response_timing", tags);
        countReceived = metrics.createCounter("received_count", tags);
        countReceivedReply = metrics.createCounter("received_reply_count", tags);
        timerReceiveReply = metrics.createTimeTracker("receive_reply_timing", tags);
        countReceivedReplyErrors = metrics.createCounter("received_reply_count_errors", tags);
        messageConvertErrors = metrics.createCounter("message_convert_count_errors", tags);

        this.metricsProcessor = metricsProcessor;
        this.logger = logger;
        this.jmsReplyQueue = jmsReplyQueue;
        this.jmsMessageConverter = jmsMessageConverter;
        this.bridgeMessageConverter = bridgeMessageConverter;
    }


    private JmsContext jms() {
        if (jms == null) {
            this.jms = jmsSupplier.get();
        }
        return jms;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public void publish(final Message message) {

        if (logger.isDebugEnabled()) logger.debug("publish called " + message);
        tryHandler.tryWithErrorCount(() -> {
            jms().producer().send(convertToJMSMessage(message));
            countPublish.increment();
        }, countPublishErrors, "Unable to send the message to the producer");

    }


    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {

        if (jms().getResponseDestination() == null) {
            throw new IllegalStateException("Response destination is not set so request/reply is not possible");
        }

        if (logger.isDebugEnabled()) logger.debug("request called " + message);

        final javax.jms.Message jmsMessage = convertToJMSMessage(message);


        tryHandler.tryWithRethrow(() -> {
            final String correlationID = jmsMessage.getJMSCorrelationID() == null ? UUID.randomUUID().toString()
                    : jmsMessage.getJMSCorrelationID();


            jmsMessage.setJMSReplyTo(jms().getResponseDestination());

            if (jmsMessage.getJMSCorrelationID() == null)
                jmsMessage.setJMSCorrelationID(correlationID);

            jms().producer().send(jmsMessage);
            if (logger.isDebugEnabled()) logger.debug("REQUEST BODY " + message.toString());

            if (logger.isDebugEnabled())
                logger.debug(String.format("CORRELATION ID: %s %s\n", correlationID, jms().getResponseDestination()));
            requestResponseMap.put(correlationID, new JMSRequestResponse(replyCallback, timeSource.getTime()));
            countRequest.increment();
        }, countRequestErrors, e -> new JMSMessageBusException("unable to send JMS request", e));

    }


    private javax.jms.Message convertToJMSMessage(final Message message) {
        return tryHandler.tryFunctionOrRethrow(message,
                m -> bridgeMessageConverter.apply(message),
                e -> new JMSMessageBusException("Unable to create JMS message", e));
    }


    private Message convertToBusMessage(final javax.jms.Message jmsMessage) {
        return tryHandler.tryFunctionOrRethrow(jmsMessage, m -> jmsMessageConverter.apply(jmsMessage), e -> {
            messageConvertErrors.increment();
            return new JMSMessageBusException("Unable to convert JMS message to Bridge Message", e);
        });

    }

    @Override
    public Optional<Message> receive() {

        return tryHandler.tryReturnOrRethrow(() -> {
            final javax.jms.Message message = jms().consumer().receiveNoWait();
            if (message != null) {
                countReceived.increment();
                return Optional.of(convertToBusMessage(message));
            } else {
                return Optional.empty();
            }
        }, e -> {
            throw new JMSMessageBusException("Error receiving message", e);
        });


    }

    public Optional<Message> receive(Duration duration) {
        return tryHandler.tryReturnOrRethrow(() -> {
            final javax.jms.Message message = jms().consumer().receive(duration.toMillis());
            if (message != null) {
                countReceived.increment();
                return Optional.of(convertToBusMessage(message));
            } else {
                return Optional.empty();
            }
        }, e -> {
            throw new JMSMessageBusException("Error receiving message", e);
        });

    }

    public void init() {
        jms();
    }

    @Override
    public void close() {
        try {
            if (jms != null) {
                jms.close();
            }
        } catch (Exception ex) {
            logger.warn("Unable to close JMS Context", ex);
        }
        jms=null;
        jmsReplyQueue.clear();
    }


    /**
     * This method gets called by bridge to process outstanding responses.
     * If the client is NATS and the Server is JMS then there will be messages from the `responseConsumer`.
     */
    private int processResponses() {


        final JmsContext jms = jms();
        if (jms == null) return 0;


        final MessageConsumer responseConsumer = jms.getResponseConsumer();

        if (responseConsumer == null) return 0;

        int[] countHolder = new int[1];

        tryHandler.tryWithRethrow(() -> {
            int count = 0;
            javax.jms.Message message;
            do {

                message = responseConsumer.receiveNoWait();
                if (message != null) {
                    count++;
                    if (logger.isDebugEnabled()) logger.debug(this.name + "::: RESPONSE FROM JMS  ");
                    final String correlationID = message.getJMSCorrelationID();
                    if (logger.isDebugEnabled())
                        logger.debug(String.format("%s ::: Process JMS Message Consumer %s \n", this.name, correlationID));
                    final Optional<JMSRequestResponse> jmsRequestResponse = Optional.ofNullable(requestResponseMap.get(correlationID));
                    final javax.jms.Message msg = message;
                    jmsRequestResponse.ifPresent(requestResponse -> {
                        requestResponse.getReplyCallback().accept(convertToBusMessage(msg));
                        /* Record metrics for duration and count. */
                        timerRequestResponse.recordTiming(this.timeSource.getTime() - requestResponse.getSentTime());
                        countRequestResponses.increment();
                    });

                    if (!jmsRequestResponse.isPresent()) {
                        countRequestResponsesMissing.increment();
                    }
                }
            }
            while (message != null);
            countHolder[0] = count;

        }, countRequestResponseErrors, e -> new JMSMessageBusException("unable to call JMS receiveNoWait", e));

        return countHolder[0];
    }

    @Override
    public int process() {
        metricsProcessor.process();
        int count = processReplies();
        return count + processResponses();
    }

    /**
     * This method gets called to process replies.
     * If the client is JMS and the Server is Nats then there will be replies to process.
     */
    private int processReplies() {
        int[] countHolder = new int[1];
        tryHandler.tryWithRethrow(() -> {
            JMSReply reply;
            final Session session = jms().getSession();
            int count = 0;
            do {
                reply = jmsReplyQueue.poll();
                if (reply != null) {

                    if (logger.isDebugEnabled())
                        logger.debug(this.name + "::: REPLY FROM SERVER IN JMS MESSAGE BUS " + reply.getReply().bodyAsString());
                    count++;
                    final byte[] messageBody = reply.getReply().getBodyBytes();
                    final String correlationId = reply.getCorrelationID();
                    final Destination jmsReplyTo = reply.getJmsReplyTo();
                    MessageProducer replyProducer;
                    try {
                        replyProducer = session.createProducer(jmsReplyTo);
                    }catch (Exception ex) {
                        logger.debug("Unable to handle creating a producer", ex);
                        return;
                    }
                    final BytesMessage jmsReplyMessage = session.createBytesMessage();
                    jmsReplyMessage.writeBytes(messageBody);
                    timerReceiveReply.recordTiming(timeSource.getTime() - reply.getSentTime());
                    countReceivedReply.increment();
                    if (logger.isDebugEnabled())
                        logger.debug(String.format("%s ::: Reply handler - %s %s %s\n", this.name,
                                reply.getReply().bodyAsString(), correlationId, replyProducer.getDestination().toString()));
                    jmsReplyMessage.setJMSCorrelationID(correlationId);
                    replyProducer.send(jmsReplyMessage);
                    replyProducer.close();
                }
            }
            while (reply != null);
            countHolder[0] = count;
        }, countReceivedReplyErrors, e -> new JMSMessageBusException("unable to process JMS Replies", e));
        return countHolder[0];
    }
}
