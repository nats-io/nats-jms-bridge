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

        Map<String, String> map = new TreeMap<>();

        for (int i = 0; i < tags.length - 1; i += 2) {
            map.put(tags[i], tags[i + 1]);
        }

        return Collections.unmodifiableMap(map);

    }

    @Override
    public Gauge createGauge(final String name, String... tags) {
        final Map<String, String> map = convertToMap(tags);
        final String id = generateId(name, map);
        final Gauge gauge = new SimpleGauge(name, map, id);
        gauges.add(gauge);
        return gauge;
    }

    private String generateId(String name, Map<String, String> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append(name);
        builder.append('_');
        for (String key : map.keySet()) {
           builder.append(key).append('_').append(map.get(key));
        }
        return builder.toString();
    }


    @Override
    public Counter createCounter(final String name, String... tags) {

        final Map<String, String> map =convertToMap(tags);
        final Counter counter = new SimpleCounter(name, map, generateId(name, map));
        counters.add(counter);
        return counter;
    }

    @Override
    public TimeTracker createTimeTracker(final String name, String... tags) {
        final Map<String, String> map =convertToMap(tags);
        final TimeTracker tt = new SimpleTimeTracker(name, timeSource, map, generateId(name, map));
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
