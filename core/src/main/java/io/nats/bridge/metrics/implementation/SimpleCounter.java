package io.nats.bridge.metrics.implementation;

import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricId;

import java.util.Map;

public class SimpleCounter implements Counter, GetMetric, MetricId {

    private final String name;
    private long count = 0;
    private final Map<String, String> tags;
    private final String id;


    public SimpleCounter(String name, Map<String, String> tags, String id) {
        this.name = name;
        this.tags = tags;
        this.id = id;
    }

    @Override
    public long getValue() {
        long c = count;
        count = 0L;
        return c;
    }

    @Override
    public void increment() {
        count++;
    }

    @Override
    public void recordCount(long count) {
        this.count += count;
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
    public String toString() {
        return String.format("%30s %10d", metricName(), getValue());
    }
}
