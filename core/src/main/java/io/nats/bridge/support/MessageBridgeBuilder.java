package io.nats.bridge.support;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;

import java.util.Queue;

public class MessageBridgeBuilder {

    private MessageBus sourceBus;
    private MessageBus destinationBus;
    private boolean requestReply=true;
    private String name;
    private Queue<MessageBridgeImpl.MessageBridgeRequestReply> replyMessageQueue;

    public MessageBus getSourceBus() {
        if (sourceBus == null) throw new IllegalStateException("Source Bus must be set");
        return sourceBus;
    }

    public MessageBridgeBuilder withSourceBus(final MessageBus sourceBus) {
        this.sourceBus = sourceBus;
        return this;
    }

    public MessageBus getDestinationBus() {
        if (destinationBus == null) throw new IllegalStateException("Destination Bus must be set");
        return destinationBus;
    }

    public MessageBridgeBuilder withDestinationBus(MessageBus destinationBus) {
        this.destinationBus = destinationBus;
        return this;
    }

    public boolean isRequestReply() {
        return requestReply;
    }

    public MessageBridgeBuilder withRequestReply(boolean requestReply) {
        this.requestReply = requestReply;
        return this;
    }

    public String getName() {
        if (name == null) throw new IllegalStateException("Name must be set");
        return name;
    }

    public MessageBridgeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Queue<MessageBridgeImpl.MessageBridgeRequestReply> getReplyMessageQueue() {
        return replyMessageQueue;
    }

    public MessageBridgeBuilder withReplyMessageQueue(Queue<MessageBridgeImpl.MessageBridgeRequestReply> replyMessageQueue) {
        this.replyMessageQueue = replyMessageQueue;
        return this;
    }

    public MessageBridge build() {
        return new MessageBridgeImpl(getName(), getSourceBus(), getDestinationBus(), isRequestReply(), getReplyMessageQueue());
    }

    public static MessageBridgeBuilder builder() {
        return new MessageBridgeBuilder();
    }
}
