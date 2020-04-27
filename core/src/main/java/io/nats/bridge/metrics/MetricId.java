package io.nats.bridge.metrics;

import java.util.Map;


public interface MetricId {

    String metricName();

    Map<String, String> dimensions();
}
