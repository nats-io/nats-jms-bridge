package io.nats.bridge.nats;

import io.nats.bridge.messages.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.bridge.util.SupplierWithException;
import io.nats.client.Connection;
import io.nats.client.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class NatsMessageBus implements MessageBus {

    private final Connection connection;
    private final String subject;
    private final Subscription subscription;
    private final ExecutorService pool;
    private final ExceptionHandler tryHandler;


    //TODO create NatsMessageBusBuilder.


    public NatsMessageBus(final String subject, final Connection connection, final String queueGroup,
                          final ExecutorService pool, final ExceptionHandler tryHandler) {

        System.out.println("SUBJECT" + subject);
        this.connection = connection;
        this.subject = subject;
        this.pool = pool;
        this.subscription = connection.subscribe(subject, queueGroup);
        this.tryHandler = tryHandler;
    }


    @Override
    public void publish(final Message message) {
        connection.publish(subject, message.getMessageBytes());
    }

    @Override
    public void publish(String message) {
        connection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void request(final Message message, final Consumer<Message> replyCallback) {

        final CompletableFuture<io.nats.client.Message> future = connection.request(subject, message.getMessageBytes());


        //TODO change the JAVA API to use a callback or use a queue reaction. Use future.isDone() and some mapping. Not cool. Need a callback here.  :)

        //TODO workaround just use Thread for now.. maybe use completeAsync which came out in Java 9. But callback is better. IMO.

        // or just use https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html#completeAsync-java.util.function.Supplier-
        // or https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html#completeAsync-java.util.function.Supplier-java.util.concurrent.Executor-
        // both of the above came out in Java 9

        pool.submit(() -> {
                    tryHandler.tryWithLog(() -> {
                        final io.nats.client.Message replyMessage = future.get();
                        replyCallback.accept(MessageBuilder.builder().buildFromBytes(replyMessage.getData()));
                    }, "Unable to handle nats reply");
                }
        );
    }

    @Override
    public Optional<Message> receive() {
        return tryHandler.tryReturnOrRethrow((SupplierWithException<Optional<Message>>) () -> {
            io.nats.client.Message message = subscription.nextMessage(Duration.ofMillis(1));

            if (message != null) {
                //TODO for now, just always send a StringMessage. Later create a Function<NatMessage, BridgeMessage> and we may need builder like JMS.
                // Also, we might be using JSON to send a message with headers. see https://github.com/nats-io/nats-jms-mq-bridge/issues/20
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

    }
}
