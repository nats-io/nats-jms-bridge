package io.nats.bridge.metrics.implementation;

import io.nats.bridge.metrics.Gauge;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.MetricId;

import java.util.Map;

public class SimpleGauge implements Gauge, GetMetric, MetricId {
    private final String name;
    private long level;
    private final Map<String, String> tags;
    private final String id;

    public SimpleGauge(String name, Map<String, String> tags, String id) {
        this.name = name;
        this.tags = tags;
        this.id = id;
    }

    @Override
    public void recordLevel(long level) {
        this.level = level;
    }

    @Override
    public long getValue() {
        return level;
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
