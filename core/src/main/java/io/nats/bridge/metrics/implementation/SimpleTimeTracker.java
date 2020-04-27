package io.nats.bridge.metrics.implementation;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricId;
import io.nats.bridge.metrics.TimeTracker;

import java.util.Map;

public class SimpleTimeTracker implements MetricId, TimeTracker, GetMetric {

    private final String name;
    private long timeHolder;
    private final TimeSource timeSource;
    private final Map<String, String> tags;
    private final String id;

    public SimpleTimeTracker(final String name, final TimeSource timeSource, Map<String, String> tags, String id) {
        this.name = name;
        this.timeSource = timeSource;
        this.tags = tags;
        this.id = id;
    }

    @Override
    public long getValue() {
        return timeHolder;
    }

    @Override
    public String metricName() {
        return name;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Map<String, String> dimensions() {
        return tags;
    }

    @Override
    public void recordTiming(long duration) {
        timeHolder = duration;
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
