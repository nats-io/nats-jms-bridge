package io.nats.bridge.metrics.implementation;

import io.nats.bridge.metrics.Gauge;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricName;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleGauge implements Gauge, GetMetric, MetricName {

    private final String name;
    private final AtomicLong levelHolder = new AtomicLong();

    public SimpleGauge(String name) {
        this.name = name;
    }

    @Override
    public void recordLevel(long level) {
        levelHolder.set(level);
    }

    @Override
    public long getValue() {
        return levelHolder.get();
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
