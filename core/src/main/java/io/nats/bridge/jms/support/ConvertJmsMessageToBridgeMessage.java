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
import java.util.Queue;

public class ConvertJmsMessageToBridgeMessage implements FunctionWithException<javax.jms.Message, Message> {

    private final TimeSource timeSource;
    private final java.util.Queue<JMSReply> jmsReplyQueue; // = new LinkedTransferQueue<>();
    private final ExceptionHandler tryHandler;
    private final String name;


    public ConvertJmsMessageToBridgeMessage(final ExceptionHandler tryHandler, final TimeSource timeSource, final Queue<JMSReply> jmsReplyQueue, String name) {
        this.timeSource = timeSource;
        this.jmsReplyQueue = jmsReplyQueue;
        this.tryHandler = tryHandler;
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

        if (jmsReplyTo != null) {
            return MessageBuilder.builder().withBody(bodyBytes).withReplyHandler(reply -> tryHandler.tryWithRethrow(() -> {
                enqueueReply(startTime, reply, jmsMessage.getJMSCorrelationID(), jmsReplyTo);
            }, e -> {
                throw new JMSMessageBusException("Unable to send to JMS reply", e);
            })).withCreator(name).build();
        } else {
            return MessageBuilder.builder().withNoReplyHandler("CONVERT JMS TO BRIDGE MESSAGE NO JMS REPLY TO").withBody(bodyBytes).build();
        }
    }


}
