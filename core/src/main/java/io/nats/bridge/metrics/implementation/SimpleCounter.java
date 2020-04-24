package io.nats.bridge.metrics.implementation;

import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricName;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleCounter implements Counter, GetMetric, MetricName {

    private final String name;
    private final AtomicLong levelHolder = new AtomicLong();


    public SimpleCounter(String name) {
        this.name = name;
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
    public String toString() {
        return String.format("%30s %10d", metricName(), getValue());
    }
}
