package io.nats.bridge.metrics;

public interface Output {

    default void println(String message) {
        System.out.println(message);
    }
}
