package io.nats.bridge.nats;

public class NatsMessageBusException extends RuntimeException {
    public NatsMessageBusException(String message) {
        super(message);
    }

    public NatsMessageBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
