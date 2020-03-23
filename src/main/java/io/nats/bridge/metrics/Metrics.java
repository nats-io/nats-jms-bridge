package io.nats.bridge.metrics;

import java.util.Collection;

public interface Metrics {

    Gauge createGauge(String name);
    Counter createCounter(String name);
    TimeTracker createTimeTracker(String name);

    Collection<Counter> counters();
    Collection<Gauge> gauges();
    Collection<TimeTracker> timers();
}
