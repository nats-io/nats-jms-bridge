package io.nats.bridge.jms.support;

import io.nats.bridge.Message;

import java.util.function.Consumer;

public class JMSRequestResponse {
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
