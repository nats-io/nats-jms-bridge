package io.nats.bridge.support;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.Transformers;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import static java.util.Collections.emptyList;

public class MessageBridgeBuilder {

    private MessageBus sourceBus;
    private MessageBus destinationBus;
    private boolean requestReply=true;
    private String name;
    private Queue<MessageBridgeImpl.MessageBridgeRequestReply> replyMessageQueue;

    private MessageBusBuilder sourceBusBuilder;
    private MessageBusBuilder destinationBusBuilder;
    private List<String> transforms= emptyList();
    private List<String> replyTransforms= emptyList();

    private Map<String, TransformMessage> transformers;

    public MessageBusBuilder getSourceBusBuilder() {
        return sourceBusBuilder;
    }

    public List<String> getReplyTransforms() {
        return replyTransforms;
    }

    public MessageBridgeBuilder withReplyTransforms(final List<String> replyTransforms) {
        this.replyTransforms = replyTransforms;
        return this;
    }

    public List<String> getTransforms() {
        return transforms;
    }

    public MessageBridgeBuilder withTransforms(List<String> transforms) {
        this.transforms = transforms;
        return this;
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

    public Map<String, TransformMessage> getTransformers() {
        if (transformers == null) {
            transformers = Transformers.loadTransforms();
        }
        return transformers;
    }

    public MessageBridgeBuilder withTransformers(Map<String, TransformMessage> transformers) {
        this.transformers = transformers;
        return this;
    }

    public MessageBridge build() {
        return new MessageBridgeImpl(getName(), getSourceBus(), getDestinationBus(), isRequestReply(),
                getReplyMessageQueue(), getTransforms(), getReplyTransforms(), getTransformers());
    }

    public static MessageBridgeBuilder builder() {
        return new MessageBridgeBuilder();
    }
}
