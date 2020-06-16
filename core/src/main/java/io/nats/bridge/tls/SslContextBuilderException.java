package io.nats.bridge.tls;

public class SslContextBuilderException extends RuntimeException {
    public SslContextBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SslContextBuilderException(String message) {
        super(message);
    }
}
