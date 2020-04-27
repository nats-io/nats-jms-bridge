package io.nats.bridge.metrics.implementation;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.Gauge;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.TimeTracker;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleMetrics implements Metrics {

    private final TimeSource timeSource;
    private final CopyOnWriteArrayList<Gauge> gauges = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Counter> counters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TimeTracker> timers = new CopyOnWriteArrayList<>();

    public SimpleMetrics(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    Map<String, String> convertToMap(String[] tags) {

        if (tags.length % 2 != 0) {
            throw new IllegalStateException("Tags must be key value pairs so must be an even number");
        }

        Map<String, String> map = new HashMap<>(tags.length / 2);

        for (int i = 0; i < tags.length - 1; i += 2) {
            map.put(tags[i], tags[i + 1]);
        }

        return Collections.unmodifiableMap(map);

    }

    @Override
    public Gauge createGauge(final String name, String... tags) {


        final Gauge gauge = new SimpleGauge(name, convertToMap(tags));
        gauges.add(gauge);
        return gauge;
    }

    @Override
    public Counter createCounter(final String name, String... tags) {

        final Counter counter = new SimpleCounter(name, convertToMap(tags));
        counters.add(counter);
        return counter;
    }

    @Override
    public TimeTracker createTimeTracker(final String name, String... tags) {
        final TimeTracker tt = new SimpleTimeTracker(name, timeSource, convertToMap(tags));
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
