package io.nats.bridge.metrics.implementation;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricId;
import io.nats.bridge.metrics.TimeTracker;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleTimeTracker implements MetricId, TimeTracker, GetMetric {

    private final String name;
    private final AtomicLong timeHolder = new AtomicLong();
    private final TimeSource timeSource;
    private final Map<String, String> tags;

    public SimpleTimeTracker(final String name, final TimeSource timeSource, Map<String, String> tags) {
        this.name = name;
        this.timeSource = timeSource;
        this.tags = tags;
    }

    @Override
    public long getValue() {
        return timeHolder.get();
    }

    @Override
    public String metricName() {
        return name;
    }

    @Override
    public Map<String, String> dimensions() {
        return tags;
    }

    @Override
    public void recordTiming(long duration) {
        timeHolder.set(duration);
    }

    @Override
    public void recordTiming(final Runnable runnable) {
        long startTime = timeSource.getTime();
        runnable.run();
        long endTime = timeSource.getTime();
        recordTiming(endTime - startTime);
    }

    @Override
    public String toString() {
        return String.format("%30s %10d", metricName(), getValue());
    }
}
