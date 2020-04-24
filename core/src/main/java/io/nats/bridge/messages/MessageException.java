package io.nats.bridge.messages;

public class MessageException extends RuntimeException {

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageException(String message) {
        super(message);
    }
}
