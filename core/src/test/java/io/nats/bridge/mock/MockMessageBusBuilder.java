package io.nats.bridge.mock;

import io.nats.bridge.TimeSource;
import io.nats.bridge.messages.Message;
import io.nats.bridge.metrics.Metrics;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class MockMessageBusBuilder {


    private String name;
    private TimeSource timeSource;
    private Metrics metrics;
    private BlockingQueue<Message> publishQueue;
    private BlockingQueue<MockMessageBus.RequestReply> requestReplyQueue;
    private BlockingQueue<MockMessageBus.Reply> repliesQueue;
    private BlockingQueue<Message> receiveQueue;
    private BlockingQueue<MockMessageBus.ReplyTo> replyToQueue;
    private BlockingQueue<Message> replyOutQueue;

    public String getName() {
        return name;
    }

    public MockMessageBusBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public MockMessageBusBuilder withTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
        return this;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public MockMessageBusBuilder withMetrics(Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public BlockingQueue<Message> getPublishQueue() {
        return publishQueue;
    }

    public MockMessageBusBuilder withPublishQueue(BlockingQueue<Message> publishQueue) {
        this.publishQueue = publishQueue;
        return this;
    }

    public BlockingQueue<MockMessageBus.RequestReply> getRequestReplyQueue() {
        return requestReplyQueue;
    }

    public MockMessageBusBuilder withRequestReplyQueue(BlockingQueue<MockMessageBus.RequestReply> requestReplyQueue) {
        this.requestReplyQueue = requestReplyQueue;
        return this;
    }

    public BlockingQueue<MockMessageBus.Reply> getRepliesQueue() {
        return repliesQueue;
    }

    public MockMessageBusBuilder withRepliesQueue(BlockingQueue<MockMessageBus.Reply> repliesQueue) {
        this.repliesQueue = repliesQueue;
        return this;
    }

    public BlockingQueue<Message> getReceiveQueue() {
        return receiveQueue;
    }

    public MockMessageBusBuilder withReceiveQueue(BlockingQueue<Message> receiveQueue) {
        this.receiveQueue = receiveQueue;
        return this;
    }

    public BlockingQueue<MockMessageBus.ReplyTo> getReplyToQueue() {
        return replyToQueue;
    }

    public MockMessageBusBuilder withReplyToQueue(BlockingQueue<MockMessageBus.ReplyTo> replyToQueue) {
        this.replyToQueue = replyToQueue;
        return this;
    }

    public BlockingQueue<Message> getReplyOutQueue() {
        return replyOutQueue;
    }

    public MockMessageBusBuilder withReplyOutQueue(BlockingQueue<Message> replyOutQueue) {
        this.replyOutQueue = replyOutQueue;
        return this;
    }

    public MockMessageBus build() {
        return new MockMessageBus(getName(), getTimeSource(), getMetrics(), getPublishQueue(), getRequestReplyQueue(), getRepliesQueue(),
                getReceiveQueue(), getReplyToQueue(), getReplyOutQueue());
    }

    public static MockMessageBusBuilder builder() {
        return new MockMessageBusBuilder();
    }

}
