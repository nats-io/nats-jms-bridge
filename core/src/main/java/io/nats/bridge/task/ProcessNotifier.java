package io.nats.bridge.task;

public interface ProcessNotifier {
    boolean stopRunning();
    default boolean keepRunning() {
        return !stopRunning();
    }
    void notifyStopped();
    void notifyStoppedByException(Exception ex);
    void notifyStoppedByError(Throwable ex);
    void notifyStarted();

    boolean wasStarted();
    boolean wasStopped();
    boolean wasError();
}
