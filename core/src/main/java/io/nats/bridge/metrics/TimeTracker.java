package io.nats.bridge.metrics;

public interface TimeTracker extends MetricId, GetMetric {

    /**
     * This is used to record timings.
     * This would be things like how long did it take this service to call this remote service.
     *
     * @param duration duration
     */
    void recordTiming(long duration);

    void recordTiming(Runnable runnable);

}
