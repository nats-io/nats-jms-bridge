package io.nats.bridge;

import java.io.Closeable;
import java.io.IOException;

public interface MessageBridge extends Closeable {
    String name();

    int process();

    @Override
    void close() throws IOException;
}
