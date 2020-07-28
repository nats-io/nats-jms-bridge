package io.nats.bridge.mock;

import io.nats.bridge.MessageBus;
import io.nats.bridge.TimeSource;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Gauge;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.TimeTracker;
import io.nats.bridge.metrics.implementation.SimpleMetrics;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MockMessageBus implements MessageBus {

    private final String name;

    private final Metrics metrics;
    private final TimeSource timeSource;
    private final Counter pubCount;
    private final Counter reqCount;
    private final Counter processCount;
    private final Counter closeCount;

    private final BlockingQueue<Message> publishQueue;
    private final BlockingQueue<RequestReply> requestReplyQueue;
    private final BlockingQueue<Reply> repliesQueue;
    private final ConcurrentHashMap<String, RequestReply> markerToRequestReplyMap = new ConcurrentHashMap<>();
    private final Gauge publishQueueSize;
    private final Gauge requestReplyQueueSize;
    private final Gauge markerToRequestReplySize;
    private final Gauge replyQueueSize;
    private final TimeTracker responseTime;
    private final BlockingQueue<Message> receiveQueue;
    private final Counter receiveCount;
    private final BlockingQueue<ReplyTo> replyToQueue;
    private final Gauge replyToSize;
    private final TimeTracker timerReceiveReply;
    private final Counter countReceivedReply;
    private final BlockingQueue<Message> replyOutQueue;


    public MockMessageBus(final String name, final TimeSource timeSource, final Metrics metrics,
                          final BlockingQueue<Message> publishQueue,
                          final BlockingQueue<RequestReply> requestReplyQueue,
                          final BlockingQueue<Reply> repliesQueue,
                          final BlockingQueue<Message> receiveQueue,
                          final BlockingQueue<ReplyTo> replyToQueue,
                          final BlockingQueue<Message> replyOutQueue) {
        this.name = name;
        this.timeSource = (timeSource == null) ? System::currentTimeMillis : timeSource;
        this.metrics = (metrics == null) ? new SimpleMetrics(timeSource) : metrics;
        this.publishQueue = (publishQueue == null) ? new LinkedTransferQueue<>() : publishQueue;
        this.requestReplyQueue = (requestReplyQueue == null) ? new LinkedTransferQueue<>() : requestReplyQueue;
        this.repliesQueue = (repliesQueue == null) ? new LinkedTransferQueue<>() : repliesQueue;
        this.replyToQueue = (replyToQueue == null) ? new LinkedTransferQueue<>() : replyToQueue;

        this.receiveQueue = (receiveQueue == null) ? new LinkedTransferQueue<>() : receiveQueue;
        this.replyOutQueue = (replyOutQueue == null) ? new LinkedTransferQueue<>() : replyOutQueue;


        pubCount = this.metrics.createCounter("pub_count");
        reqCount = this.metrics.createCounter("req_count");
        processCount = this.metrics.createCounter("proc_count");

        receiveCount = this.metrics.createCounter("receive_count");
        closeCount = this.metrics.createCounter("close_count");
        publishQueueSize = this.metrics.createGauge("pub_size");
        requestReplyQueueSize = this.metrics.createGauge("req_rep_size");
        replyQueueSize = this.metrics.createGauge("reply_size");

        replyToSize = this.metrics.createGauge("reply_to_size");
        markerToRequestReplySize = this.metrics.createGauge("marker_req_rep_size");
        responseTime = this.metrics.createTimeTracker("response_time");

        timerReceiveReply = this.metrics.createTimeTracker("reply_time");
        countReceivedReply = this.metrics.createCounter("received_reply_count");


    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public void publish(final Message message) {
        pubCount.increment();
        publishQueue.add(message);

    }

    public static class ReplyTo {
        private final String replyTo;
        private final Message reply;
        private final Message request;
        private final long startTime;

        ReplyTo(long startTime, String replyTo, Message reply,  Message request) {
            this.replyTo = replyTo;
            this.reply = reply;
            this.startTime = startTime;
            this.request = request;
        }
    }


    public static class RequestReply {
        private final Message requestMessage;
        private final Consumer<Message> originalReplyCallback;
        private final long startTime;
        private final Consumer<Message> replyCallback;
        private final AtomicBoolean done = new AtomicBoolean();

        public RequestReply(final Message requestMessage, final Consumer<Message> replyCallback, long startTime) {
            this.requestMessage = requestMessage;
            this.originalReplyCallback = replyCallback;
            this.replyCallback = reply -> {
                originalReplyCallback.accept(reply);
                done.set(true);
            };
            this.startTime = startTime;
        }

        public Consumer<Message> getReplyCallback() {
            return replyCallback;
        }

        public boolean isDone() {
            return done.get();
        }

        public long getStartTime() {
            return startTime;
        }
    }

    public static class Reply {
        private final Message reply;
        private final String marker;
        private final Message requestMessage;

        public Reply(Message reply, String marker,final Message requestMessage) {
            this.reply = reply;
            this.marker = marker;
            this.requestMessage = requestMessage;
        }
    }

    @Override
    public void request(final Message requestMessage, final Consumer<Message> replyCallback) {
        reqCount.increment();
        final String marker = UUID.randomUUID().toString();
        final RequestReply rr = new RequestReply(requestMessage, replyCallback, timeSource.getTime());

        markerToRequestReplyMap.put(marker, rr);
        requestReplyQueue.add(rr);

        publishQueue.add(MessageBuilder.builder().withCorrelationID(marker)
                .withReplyHandler(replyMessage -> addReply(marker, replyMessage, requestMessage))
                .buildFromBytes(requestMessage.getMessageBytes()));


    }

    private boolean addReply(final String marker, final Message replyMessage, final Message requestMessage) {
        return repliesQueue.add(new Reply(replyMessage, marker, requestMessage));
    }

    @Override
    public Optional<Message> receive() {
        final Message poll = receiveQueue.poll();

        return doReceive(poll);


    }

    @Override
    public Optional<Message> receive(Duration duration) {
        try {
            final Message poll = receiveQueue.poll(duration.toMillis(), TimeUnit.MILLISECONDS);
            return doReceive(poll);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<Message> doReceive(final Message message) {
        if (message == null) {
            return Optional.empty();
        } else {
            receiveCount.increment();

            if (message.correlationID() != null) {
                return Optional.of(MessageBuilder.builder().withReplyHandler(reply -> replyUsingReplyTo(timeSource.getTime(), message.correlationID(), reply, message))
                        .withCreator(name).withCorrelationID(message.correlationID()).buildFromBytes(message.getMessageBytes()));
            } else {
                return Optional.of(message);
            }

        }
    }

    private void replyUsingReplyTo(final long startTime, final String correlationID, final Message reply, final Message request) {
        replyToQueue.add(new ReplyTo(startTime, correlationID, reply, request));
    }

    @Override
    public void close() {
        closeCount.increment();

    }

    int process = 0;


    @Override
    public int process() {
        process++;

        int count = 0;
        processCount.increment();

        replyQueueSize.recordLevel(repliesQueue.size());

        Reply reply = repliesQueue.poll();
        while (reply != null) {
            final RequestReply requestReply = markerToRequestReplyMap.get(reply.marker);
            if (requestReply != null) {
                markerToRequestReplyMap.remove(reply.marker);
                requestReply.getReplyCallback().accept(reply.reply);
                responseTime.recordTiming(timeSource.getTime() - requestReply.getStartTime());
            }
            count++;
            reply = repliesQueue.poll();
        }

        int size = requestReplyQueue.size();

        for (int i = 0; i < size; i++) {
            final RequestReply poll = requestReplyQueue.poll();
            if (poll == null) {
                break;
            }
            if (!poll.isDone()) {
                requestReplyQueue.add(poll);
            }
        }

        if (process % 10 == 0) {
            publishQueueSize.recordLevel(publishQueue.size());
            markerToRequestReplySize.recordLevel(markerToRequestReplyMap.size());
            requestReplyQueueSize.recordLevel(requestReplyQueue.size());
            replyToSize.recordLevel(replyToQueue.size());
        }

        return count + processReplyToQueue();
    }


    private int processReplyToQueue() {
        int count = 0;
        ReplyTo reply = replyToQueue.poll();
        while (reply != null) {
            reply.request.reply(reply.reply);
            count++;
            timerReceiveReply.recordTiming(timeSource.getTime() - reply.startTime);
            countReceivedReply.increment();
            reply = replyToQueue.poll();
        }
        return count;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public Counter getPubCount() {
        return pubCount;
    }

    public Counter getProcessCount() {
        return processCount;
    }

    public Counter getCloseCount() {
        return closeCount;
    }

    public BlockingQueue<Message> getPublishQueue() {
        return publishQueue;
    }

    public BlockingQueue<RequestReply> getRequestReplyQueue() {
        return requestReplyQueue;
    }

    public BlockingQueue<Reply> getRepliesQueue() {
        return repliesQueue;
    }

    public Gauge getPublishQueueSize() {
        return publishQueueSize;
    }

    public Gauge getRequestReplyQueueSize() {
        return requestReplyQueueSize;
    }

    public Gauge getMarkerToRequestReplySize() {
        return markerToRequestReplySize;
    }

    public Gauge getReplyQueueSize() {
        return replyQueueSize;
    }

    public Counter getReqCount() {
        return reqCount;
    }

    public TimeTracker getResponseTime() {
        return responseTime;
    }

    public BlockingQueue<Message> getReceiveQueue() {
        return receiveQueue;
    }

    public Counter getReceiveCount() {
        return receiveCount;
    }

    public BlockingQueue<ReplyTo> getReplyToQueue() {
        return replyToQueue;
    }

    public Gauge getReplyToSize() {
        return replyToSize;
    }

    public TimeTracker getTimerReceiveReply() {
        return timerReceiveReply;
    }

    public Counter getCountReceivedReply() {
        return countReceivedReply;
    }

    public BlockingQueue<Message> getReplyOutQueue() {
        return replyOutQueue;
    }
}
