package io.nats.bridge.jms.support;

import io.nats.bridge.messages.StringMessage;

import javax.jms.Destination;

public class JMSReply {
    private final StringMessage reply;
    private final String correlationID;
    private final Destination jmsReplyTo;
    private final long sentTime;

    public JMSReply(long sentTime, final StringMessage reply, final String correlationID, final Destination jmsReplyTo) {
        this.sentTime = sentTime;
        this.reply = reply;
        this.correlationID = correlationID;
        this.jmsReplyTo = jmsReplyTo;
    }

    public StringMessage getReply() {
        return reply;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public Destination getJmsReplyTo() {
        return jmsReplyTo;
    }

    public long getSentTime() {
        return sentTime;
    }
}
