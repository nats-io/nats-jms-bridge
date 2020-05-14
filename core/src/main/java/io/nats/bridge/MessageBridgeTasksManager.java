package io.nats.bridge;
import java.io.Closeable;


public interface MessageBridgeTasksManager extends Closeable {

    String name();
    void start();

    @Override
    void close();

    boolean isHealthy();

    Exception lastError();

    boolean wasStarted();

}
