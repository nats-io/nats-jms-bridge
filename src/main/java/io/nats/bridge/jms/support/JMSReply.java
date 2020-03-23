package io.nats.bridge.jms.support;

import io.nats.bridge.StringMessage;

import javax.jms.Destination;

public class JMSReply {
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

    public JMSReply(final StringMessage reply, final String correlationID, final Destination jmsReplyTo) {
        this.reply = reply;
        this.correlationID = correlationID;
        this.jmsReplyTo = jmsReplyTo;
    }
}
