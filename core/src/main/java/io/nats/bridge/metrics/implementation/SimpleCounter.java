package io.nats.bridge.metrics.implementation;

import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleCounter implements Counter, GetMetric, MetricId {

    private final String name;
    private final AtomicLong levelHolder = new AtomicLong();
    private final Map<String, String> tags;


    public SimpleCounter(String name, Map<String, String>  tags) {
        this.name = name;
        this.tags = tags;
    }

    @Override
    public long getValue() {
        return levelHolder.get();
    }

    @Override
    public void increment() {
        levelHolder.incrementAndGet();
    }

    @Override
    public void recordCount(long count) {
        levelHolder.addAndGet(count);
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
    public String toString() {
        return String.format("%30s %10d", metricName(), getValue());
    }
}
