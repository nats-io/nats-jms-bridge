package io.nats.bridge.metrics;

import java.util.Collection;

public interface Metrics {

    static String[] tags (String... tags) {
        return tags;
    }

    Gauge createGauge(String name, String... tags);

    Counter createCounter(String name, String... tags);

    TimeTracker createTimeTracker(String name, String... tags);

    Collection<Counter> counters();

    Collection<Gauge> gauges();

    Collection<TimeTracker> timers();
}
