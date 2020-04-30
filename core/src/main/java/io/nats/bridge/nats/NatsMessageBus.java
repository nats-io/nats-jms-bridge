package io.nats.bridge.nats;

import io.nats.bridge.MessageBus;
import io.nats.bridge.TimeSource;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.TimeTracker;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.client.Connection;
import io.nats.client.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;

//import java.util.concurrent.ExecutorService;


public class NatsMessageBus implements MessageBus {

    final Duration NOW = Duration.ofMillis(1L);
    private final Connection connection;
    private final String subject;
    private final Subscription subscription;
    //private final ExecutorService pool;
    private final ExceptionHandler tryHandler;
    private final Queue<NatsReply> replyQueue;
    private final Queue<NatsReply> replyQueueNotDone;

    private final Queue<ReplyTo> replyToQueue;
    private final Metrics metrics;

    private final Counter countReceivedReply;
    private final Counter countReceivedReplyErrors;
    private final Counter countPublish;
    private final Counter countReceived;
    private final Counter countRequest;
    private final Counter countRequestResponses;
    private final TimeTracker timerRequestResponse;
    private final TimeTracker timerReceiveReply;
    private boolean stopped = false;


    //TODO create NatsMessageBusBuilder.
    private final MetricsProcessor metricsProcessor;
    private final TimeSource timeSource;
    private final String name;

    public NatsMessageBus(final String name, final String subject, final Connection connection,
                          final String queueGroup,
                          //final ExecutorService pool, 
                          final ExceptionHandler tryHandler,
                          final Queue<NatsReply> replyQueue,
                          final Queue<NatsReply> replyQueueNotDone,
                          final TimeSource timeSource,
                          final Metrics metrics,
                          final MetricsProcessor metricsProcessor) {

        //ystem.out.println("SUBJECT" + subject);
        this.connection = connection;
        this.subject = subject;
        //this.pool = pool;
        this.subscription = connection.subscribe(subject, queueGroup);
        this.tryHandler = tryHandler;
        this.replyQueue = replyQueue;
        this.timeSource = timeSource;
        this.replyQueueNotDone = replyQueueNotDone;
        this.metrics = metrics;
        this.metricsProcessor = metricsProcessor;
        this.name = name.toLowerCase().replace(".", "_").replace(" ", "_").replace("-", "_");

        final String[] tags = Metrics.tags("name", "name_" + this.name, "mb_type", "nats_mb", "subject", subject);

        countPublish = metrics.createCounter("publish_count", tags);
        countRequest = metrics.createCounter("request_count", tags);
        countRequestResponses = metrics.createCounter("request_response_count", tags);
        timerRequestResponse = metrics.createTimeTracker("request_response_timing", tags);
        countReceived = metrics.createCounter("received_count", tags);
        countReceivedReply = metrics.createCounter("received_reply_count", tags);
        timerReceiveReply = metrics.createTimeTracker("receive_reply_timing", tags);
        countReceivedReplyErrors = metrics.createCounter("received_reply_count_errors", tags);
        replyToQueue = new LinkedTransferQueue<>();

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
        countPublish.increment();
        connection.publish(subject, message.getMessageBytes());
    }

    @Override
    public void publish(String message) {
        countPublish.increment();
        connection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }


    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {
        countRequest.increment();
        final CompletableFuture<io.nats.client.Message> future = connection.request(subject, message.getMessageBytes());

        if (!replyQueue.add(new NatsReply(timeSource.getTime(), replyCallback, future))) {
            throw new NatsMessageBusException("Unable to add to reply queue");
        }
    }

    @Override
    public Optional<Message> receive(final Duration duration) {
        if (stopped) return Optional.empty();

        return doReceive(duration);
    }

    @Override
    public Optional<Message> receive() {
        return doReceive(NOW);
    }

    private Optional<Message> doReceive(final Duration duration) {
        return tryHandler.tryReturnOrRethrow(() -> {
            io.nats.client.Message message = subscription.nextMessage(duration);
            if (message != null) {
                countReceived.increment();
                return convertMessage(message);
            } else {
                return Optional.empty();
            }
        }, e -> {
            throw new NatsMessageBusException("unable to get next message from nats bus", e);
        });

    }

    private void replyUsingReplyTo(long startTime, final String replyTo, final Message reply) {

        replyToQueue.add(new ReplyTo(startTime, replyTo, reply));
    }

    private Optional<Message> convertMessage(io.nats.client.Message message) {
        final String replyTo = message.getReplyTo();

        if (replyTo != null) {
            final long startTime = timeSource.getTime();
            return Optional.of(
                    MessageBuilder.builder().withReplyHandler(reply -> replyUsingReplyTo(startTime, replyTo, reply))
                            .withCreator(name).buildFromBytes(message.getData())
            );
        } else {
            final Message bridgeMessage = MessageBuilder.builder().withNoReplyHandler("NATS MESSAGE BUS NO REPLY TO CONVERT MESSAGE NATS TO BRIDGE").withCreator(name).buildFromBytes(message.getData());
            return Optional.of(bridgeMessage);
        }
    }

    @Override
    public void close() {
        tryHandler.tryWithLog(() -> {
            stopped = true;
            connection.drain(Duration.ofSeconds(30)).get();

        }, "Can't drain and close nats connection " + subject);
    }

    @Override
    public int process() {
        metricsProcessor.process();
        int count = processReplyToQueue();
        return count + processResponses();
    }

    private int processReplyToQueue() {
        int count = 0;
        ReplyTo reply = replyToQueue.poll();
        while (reply != null) {
            connection.publish(reply.replyTo, reply.reply.getMessageBytes());
            count++;
            timerReceiveReply.recordTiming(timeSource.getTime() - reply.startTime);
            countReceivedReply.increment();
            reply = replyToQueue.poll();
        }
        return count;
    }

    private int processResponses() {
        int[] countHolder = new int[1];


        tryHandler.tryWithErrorCount(() -> {
            NatsReply reply = null;
            int count = 0;
            do {
                reply = replyQueue.poll();
                if (reply != null) {
                    if (reply.future.isDone()) {
                        count++;
                        final io.nats.client.Message replyMessage = reply.future.get();
                        reply.replyCallback.accept(MessageBuilder.builder().withCreator(name).withNoReplyHandler("NATS MESSAGE BUS PROCESS RESPONSE").buildFromBytes(replyMessage.getData()));
                        timerRequestResponse.recordTiming(timeSource.getTime() - reply.requestTime);
                        countRequestResponses.increment();
                    } else {
                        if (!replyQueueNotDone.add(reply)) {
                            throw new NatsMessageBusException("Unable to add to reply queue");
                        }
                    }
                }
            }
            while (reply != null);

            do {
                reply = replyQueueNotDone.poll();
                if (reply != null) {
                    if (!replyQueue.add(reply)) {
                        throw new NatsMessageBusException("Unable to add to reply queue");
                    }
                }
            }
            while (reply != null);
            replyQueueNotDone.clear();

            countHolder[0] = count;

        }, countReceivedReplyErrors, "error processing NATS receive queue for replies");

        return countHolder[0];
    }

    private static class ReplyTo {
        private final String replyTo;
        private final Message reply;
        private final long startTime;

        ReplyTo(long startTime, String replyTo, Message reply) {
            this.replyTo = replyTo;
            this.reply = reply;
            this.startTime = startTime;
        }
    }

    public static class NatsReply {

        private final long requestTime;
        private final Consumer<Message> replyCallback;
        private final CompletableFuture<io.nats.client.Message> future;


        NatsReply(final long requestTime,
                  final Consumer<Message> replyCallback,
                  final CompletableFuture<io.nats.client.Message> future) {
            this.requestTime = requestTime;
            this.replyCallback = replyCallback;
            this.future = future;
        }
    }
}
