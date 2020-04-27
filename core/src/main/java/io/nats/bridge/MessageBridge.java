package io.nats.bridge;

import io.nats.bridge.metrics.Metrics;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

public interface MessageBridge extends Closeable {
    String name();

    int process();

    int process(Duration duration);

    @Override
    void close() throws IOException;

    Metrics sourceMetrics();
    Metrics destinationMetrics();
}
