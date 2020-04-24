package io.nats.bridge.jms.support;

import io.nats.bridge.messages.Message;

import java.util.function.Consumer;

public class JMSRequestResponse {
    private final Consumer<Message> replyCallback;
    private final long sentTime;


    public JMSRequestResponse(final Consumer<Message> replyCallback, final long sentTime) {
        this.replyCallback = replyCallback;
        this.sentTime = sentTime;
    }

    public Consumer<Message> getReplyCallback() {
        return replyCallback;
    }

    public long getSentTime() {
        return sentTime;
    }

}
