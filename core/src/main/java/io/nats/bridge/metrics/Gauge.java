package io.nats.bridge.metrics;

public interface Gauge extends MetricId, GetMetric{


    /**
     * This is used to record things like the count of current threads or
     * free system memory or free disk, etc.
     * Record Level. Some systems call this a gauge.
     *
     * @param level level
     */
    void recordLevel(long level);


}
