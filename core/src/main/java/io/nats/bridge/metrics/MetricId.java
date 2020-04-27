package io.nats.bridge.metrics;

import java.util.Map;


public interface MetricId {

    String metricName();

    String id();

    Map<String, String> dimensions();
}
