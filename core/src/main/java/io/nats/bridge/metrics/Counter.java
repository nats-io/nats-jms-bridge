package io.nats.bridge.metrics;


/**
 * Collects runtime metrics.
 * This collects key performance indicators (KPI): timings, counts and levels/gauges.
 */
public interface Counter extends MetricId, GetMetric{


    /**
     * Increment a counter by 1.
     * This is a short cut for recordCount(name, 1);
     */
    default void increment() {
        recordCount(1);
    }

    /**
     * Record a count.
     * Used to record things like how many users used the site.
     *
     * @param count count to record.
     */
    void recordCount(long count);

}
