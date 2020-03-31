package io.nats.bridge.metrics.implementation;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Gauge;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.TimeTracker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleMetrics implements Metrics {

    private final TimeSource timeSource;
    private final CopyOnWriteArrayList<Gauge> gauges = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Counter> counters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TimeTracker> timers = new CopyOnWriteArrayList<>();

    public SimpleMetrics(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    public Gauge createGauge(final String name) {

        final Gauge gauge = new SimpleGauge(name);
        gauges.add(gauge);
        return gauge;
    }

    @Override
    public Counter createCounter(final String name) {

        final Counter counter = new SimpleCounter(name);
        counters.add(counter);
        return counter;
    }

    @Override
    public TimeTracker createTimeTracker(final String name) {
        final TimeTracker tt = new SimpleTimeTracker(name, timeSource);
        timers.add(tt);
        return tt;
    }

    @Override
    public Collection<Counter> counters() {
        return Collections.unmodifiableCollection(counters);
    }

    @Override
    public Collection<Gauge> gauges() {
        return Collections.unmodifiableCollection(gauges);
    }

    @Override
    public Collection<TimeTracker> timers() {
        return Collections.unmodifiableCollection(timers);
    }
}
