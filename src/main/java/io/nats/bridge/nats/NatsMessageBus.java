package io.nats.bridge.nats;

import io.nats.bridge.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.StringMessage;
import io.nats.client.Connection;
import io.nats.client.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NatsMessageBus implements MessageBus {

    private final Connection connection;
    private final String subject;
    private final Subscription subscription;
    private final ExecutorService pool;


    //TODO create NatsMessageBusBuilder.

    public NatsMessageBus(final String subject, final Connection connection) {
        this.connection = connection;
        this.subject = subject;
        this.subscription = connection.subscribe(subject);
        this.pool = Executors.newFixedThreadPool(25);
    }

    public NatsMessageBus(final String subject, final Connection connection, final String queueGroup) {
        this.connection = connection;
        this.subject = subject;
        this.subscription = connection.subscribe(subject, queueGroup);
        this.pool = Executors.newFixedThreadPool(25);
    }


    @Override
    public void publish(final Message message) {
        if (message instanceof StringMessage) {
            connection.publish(subject, ((StringMessage) message).getBody().getBytes(StandardCharsets.UTF_8));
        } else {
            throw new NatsMessageBusException("Message type not supported");
        }
    }

    @Override
    public void publish(String message) {
        connection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {
        final String msg = ((StringMessage) message).getBody();

        final CompletableFuture<io.nats.client.Message> future = connection.request(subject, msg.getBytes(StandardCharsets.UTF_8));


        //TODO change the JAVA API to use a callback or use a queue reaction. Use future.isDone() and some mapping. Not cool. Need a callback here.  :)

        //TODO workaround just use Thread for now.. maybe use completeAsync which came out in Java 9. But callback is better. IMO.

        // or just use https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html#completeAsync-java.util.function.Supplier-
        // or https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html#completeAsync-java.util.function.Supplier-java.util.concurrent.Executor-
        // both of the above came out in Java 9

        pool.submit(() -> {
            try {
                final io.nats.client.Message replyMessage = future.get();
                replyCallback.accept(new StringMessage(new String(replyMessage.getData(), StandardCharsets.UTF_8)));
            } catch (Exception e) {
                //TODO log and track metrics instead.
                e.printStackTrace();
            }
        });


    }

    @Override
    public Optional<Message> receive() {
        try {
            io.nats.client.Message message = subscription.nextMessage(Duration.ofMillis(1));
            if (message != null) {
                //TODO for now, just always send a StringMessage. Later create a Function<NatMessage, BridgeMessage> and we may need builder like JMS.
                // Also, we might be using JSON to send a message with headers. see https://github.com/nats-io/nats-jms-mq-bridge/issues/20
                final String  replyTo = message.getReplyTo();

                if (replyTo != null) {
                    final String messageBody = new String(message.getData(), StandardCharsets.UTF_8);
                    return Optional.of(new StringMessage(messageBody) {
                        @Override
                        public void reply(final Message reply) {
                            final StringMessage stringMessage = (StringMessage) reply;
                            connection.publish(replyTo, stringMessage.getBody().getBytes(StandardCharsets.UTF_8));
                        }
                    });
                } else {
                    final String messageBody = new String(message.getData(), StandardCharsets.UTF_8);
                    return Optional.of(new StringMessage(messageBody));
                }


            } else {
                return Optional.empty();
            }
        } catch (InterruptedException e) {
            throw new NatsMessageBusException("unable to get next message", e);
        }
    }

    @Override
    public void close() {
        try {
            CompletableFuture<Boolean> drain = connection.drain(Duration.ofSeconds(10));
            drain.get();
            connection.close();
        } catch (Exception e) {
            throw new NatsMessageBusException("Can't drain and close nats connection " + subject, e);
        }
    }

    @Override
    public void process() {

    }
}
