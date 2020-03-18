package io.nats.bridge;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Message Bus wraps a Nats.io and a JMS message context to send/recieve messages to the messaging systems.
 * It also encapsulates how request/reply is DONE.
 * TODO write Nats.io version
 */
public interface MessageBus extends Closeable {

    /** Publish a message.
     *
     * @param message message bus message.
     */
    void publish(Message message);

    /** Publish a string message
     *
     * @param message string message
     */
    default void publish(String message) {
        publish(new StringMessage(message));
    }

    /**
     * Perform a request/reply over nats or JMS.
     * @param message message to send
     * @param replyCallback callback.
     */
    void request(final Message message, Consumer<Message> replyCallback);

    /**
     * Perform a request/reply with strings
     * @param message message string
     * @param reply callback for reply string
     */
    default void request(final String message, final Consumer<String> reply) {
        request(new StringMessage(message), replyMessage -> reply.accept(((StringMessage) replyMessage).getBody()));
    }

    /**
     * Receives a message. The optional is none if the message is not received.
     * @return a possible message.
     */
    Optional<Message> receive();

    void close();


}
