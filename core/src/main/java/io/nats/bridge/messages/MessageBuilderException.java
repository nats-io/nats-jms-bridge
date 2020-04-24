package io.nats.bridge.messages;

public class MessageBuilderException extends RuntimeException {
    public MessageBuilderException(String message) {
        super(message);
    }

    public MessageBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
