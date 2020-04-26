package io.nats.bridge.nats;

import io.nats.bridge.messages.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.SupplierWithException;
import io.nats.client.Connection;
import io.nats.client.Subscription;
import io.nats.bridge.TimeSource;

import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.TimeTracker;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.Queue;


public class NatsMessageBus implements MessageBus {

    private final Connection connection;
    private final String subject;
    private final Subscription subscription;
    //private final ExecutorService pool;
    private final ExceptionHandler tryHandler;

    private final Queue<NatsReply> replyQueue;
    private final Queue<NatsReply> replyQueueNotDone;
    private final Metrics metrics;
    private final Counter countRequestResponseErrors;
    private final Counter countReceivedReply;
    private final Counter countReceivedReplyErrors;
    private final Counter countPublish;
    private final Counter countReceived;
    private final Counter countPublishErrors;
    private final Counter messageConvertErrors;
    private final Counter countRequest;
    private final Counter countRequestErrors;
    private final Counter countRequestResponses;
    private final Counter countRequestResponsesMissing;
    private final TimeTracker timerRequestResponse;
    private final TimeTracker timerReceiveReply;
    private final MetricsProcessor metricsProcessor;
    
    

    //TODO create NatsMessageBusBuilder.

    private final TimeSource timeSource;


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

    public NatsMessageBus(final String subject, final Connection connection, 
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

        countPublish = metrics.createCounter("publish-count");
        countPublishErrors = metrics.createCounter("publish-count-errors");
        countRequest = metrics.createCounter("request-count");
        countRequestErrors = metrics.createCounter("request-count-errors");
        countRequestResponses = metrics.createCounter("request-response-count");
        countRequestResponseErrors = metrics.createCounter("request-response-count-errors");
        countRequestResponsesMissing = metrics.createCounter("request-response-missing-count");
        timerRequestResponse = metrics.createTimeTracker("request-response-timing");
        countReceived = metrics.createCounter("received-count");
        countReceivedReply = metrics.createCounter("received-reply-count");
        timerReceiveReply = metrics.createTimeTracker("receive-reply-timing");
        countReceivedReplyErrors = metrics.createCounter("received-reply-count-errors");
        messageConvertErrors = metrics.createCounter("message-convert-count-errors");

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
        return doReceive(duration);
    }


    final Duration NOW = Duration.ofMillis(1L);
    @Override
    public Optional<Message> receive() {
        return doReceive(NOW);
    }


    private Optional<Message> doReceive(final Duration duration) {
        return tryHandler.tryReturnOrRethrow((SupplierWithException<Optional<Message>>) () -> {
            io.nats.client.Message message = subscription.nextMessage(duration);

            if (message != null) {

                countReceived.increment();

                final String replyTo = message.getReplyTo();

                if (replyTo != null) {
                    return Optional.of(
                            MessageBuilder.builder().withReplyHandler(new Consumer<Message>() {
                                @Override
                                public void accept(final Message reply) {

                                    //ystem.out.println("REPLY MESSAGE " + reply.bodyAsString() + "HEADERS" + reply.headers());
                                    connection.publish(replyTo, reply.getMessageBytes());
                                }
                            }).buildFromBytes(message.getData())
                     );
                } else {
                    final Message bridgeMessage = MessageBuilder.builder().buildFromBytes(message.getData());
                    //ystem.out.println("## Receive MESSAGE " + bridgeMessage.bodyAsString() + " " + bridgeMessage.headers());
                    return Optional.of(bridgeMessage);
                }
            } else {
                return Optional.empty();
            }
        }, e -> {
            throw new NatsMessageBusException("unable to get next message from nats bus", e);
        });

    }

    @Override
    public void close() {
        tryHandler.tryWithLog(() -> {
        }, "Can't drain and close nats connection " + subject);
    }

    @Override
    public void process() {

        tryHandler.tryWithErrorCount(() -> {
            NatsReply reply = null;
            do {
                reply = replyQueue.poll();
                if (reply != null) {
                    if (reply.future.isDone()) {
                        final io.nats.client.Message replyMessage = reply.future.get();
                        reply.replyCallback.accept(MessageBuilder.builder().buildFromBytes(replyMessage.getData()));
                        timerReceiveReply.recordTiming(timeSource.getTime() - reply.requestTime);
                        countReceivedReply.increment();
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
                if (reply!=null) {
                    if (!replyQueue.add(reply)) {
                        throw new NatsMessageBusException("Unable to add to reply queue");
                    }
                }
            }
            while (reply != null);
            replyQueueNotDone.clear();

        }, countReceivedReplyErrors, "error processing NATS receive queue for replies");

    }
}
