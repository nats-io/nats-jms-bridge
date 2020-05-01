package io.nats.bridge.jms.support;

import io.nats.bridge.TimeSource;
import io.nats.bridge.jms.JMSMessageBusException;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.FunctionWithException;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.TextMessage;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Queue;

public class ConvertJmsMessageToBridgeMessageWithHeaders implements FunctionWithException<javax.jms.Message, Message> {

    private final ExceptionHandler tryHandler;
    private final TimeSource timeSource;
    private final Queue<JMSReply> jmsReplyQueue;
    private final String name;

    public ConvertJmsMessageToBridgeMessageWithHeaders(final ExceptionHandler tryHandler, final TimeSource timeSource,
                                                       final Queue<JMSReply> jmsReplyQueue, String name) {
        this.tryHandler = tryHandler;
        this.timeSource = timeSource;
        this.jmsReplyQueue = jmsReplyQueue;
        this.name = name;
    }

    private void enqueueReply(final long sentTime, final Message reply, final String correlationID, final Destination jmsReplyTo) {
        jmsReplyQueue.add(new JMSReply(sentTime, reply, correlationID, jmsReplyTo));
    }

    private byte[] readBytesFromJMSMessage(final javax.jms.Message jmsMessage) throws Exception {

        if (jmsMessage instanceof BytesMessage) {
            final BytesMessage bytesMessage = (BytesMessage) jmsMessage;
            byte[] buffer = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(buffer);
            return buffer;
        } else if (jmsMessage instanceof TextMessage) {
            return ((TextMessage) jmsMessage).getText().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new JMSMessageBusException("Unable to read bytes from message " + jmsMessage.getClass().getName());
        }

    }

    @Override
    public Message apply(final javax.jms.Message jmsMessage) throws Exception {

        final Destination jmsReplyTo = jmsMessage.getJMSReplyTo();
        final long startTime = timeSource.getTime();

        byte[] bodyBytes = readBytesFromJMSMessage(jmsMessage);
        final MessageBuilder builder = MessageBuilder.builder().withBody(bodyBytes);
        copyHeaders(builder, jmsMessage);
        if (jmsReplyTo != null) {
            builder.withReplyHandler(reply -> tryHandler.tryWithRethrow(() -> enqueueReply(startTime, reply, jmsMessage.getJMSCorrelationID(), jmsReplyTo), e -> {
                throw new JMSMessageBusException("Unable to send to JMS reply", e);
            }));
        } else {
            builder.withNoReplyHandler("CONVERT JMS TO BRIDGE MESSAGE WITH HEADERS FOR NO JMS_REPLY_TO");
        }
        return builder.withCreator(name).build();

    }

    private void copyHeaders(final MessageBuilder builder, final javax.jms.Message jmsMessage) throws Exception {


        final Enumeration<String> propertyNames = (Enumeration<String>) jmsMessage.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            final String propertyName = propertyNames.nextElement();
            //ystem.out.println("JMS HEADER ---------------------------------------->" + propertyName);

            if (!propertyName.startsWith("JMS"))
                builder.withHeader(propertyName, jmsMessage.getObjectProperty(propertyName));
        }

        builder.withRedelivered(jmsMessage.getJMSRedelivered());
        builder.withDeliveryTime(jmsMessage.getJMSDeliveryTime());
        builder.withDeliveryMode(jmsMessage.getJMSDeliveryMode());
        builder.withType(jmsMessage.getJMSType());
        builder.withPriority(jmsMessage.getJMSPriority());
        builder.withExpirationTime(jmsMessage.getJMSExpiration());
        builder.withTimestamp(jmsMessage.getJMSTimestamp());
        builder.withCorrelationID(jmsMessage.getJMSCorrelationID());
    }

}
