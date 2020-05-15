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

    private MessageBusBuilder sourceBusBuilder;
    private MessageBusBuilder destinationBusBuilder;

    public MessageBusBuilder getSourceBusBuilder() {
        return sourceBusBuilder;
    }

    public MessageBridgeBuilder withSourceBusBuilder(MessageBusBuilder sourceBusBuilder) {
        this.sourceBusBuilder = sourceBusBuilder;
        return this;
    }

    public MessageBusBuilder getDestinationBusBuilder() {
        return destinationBusBuilder;
    }

    public MessageBridgeBuilder withDestinationBusBuilder(MessageBusBuilder destBusBuilder) {
        this.destinationBusBuilder = destBusBuilder;
        return this;
    }

    public MessageBus getSourceBus() {
        if (sourceBus == null && sourceBusBuilder == null) throw new IllegalStateException("Source Bus must be set");

        if (sourceBus == null) {
            sourceBus = getSourceBusBuilder().build();
        }
        return sourceBus;
    }

    public MessageBridgeBuilder withSourceBus(final MessageBus sourceBus) {
        this.sourceBus = sourceBus;
        return this;
    }

    public MessageBus getDestinationBus() {
        if (destinationBus == null && destinationBusBuilder == null) throw new IllegalStateException("Destination Bus must be set");
        if (destinationBus == null) {
            destinationBus = getDestinationBusBuilder().build();
        }
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
