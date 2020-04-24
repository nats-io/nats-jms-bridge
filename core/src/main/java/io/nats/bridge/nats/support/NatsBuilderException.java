package io.nats.bridge.nats.support;

public class NatsBuilderException extends RuntimeException {
    public NatsBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NatsBuilderException(String message) {
        super(message);
    }
}
