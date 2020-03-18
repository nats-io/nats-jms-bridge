package io.nats.bridge.jms;

public class JMSMessageBusException extends RuntimeException {
    public JMSMessageBusException(String message, Throwable cause) {
        super(message, cause);
    }

    public JMSMessageBusException(String message) {
        super(message);
    }
}
