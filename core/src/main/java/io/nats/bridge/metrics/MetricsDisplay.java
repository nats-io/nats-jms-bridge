package io.nats.bridge.metrics;

import io.nats.bridge.TimeSource;

import java.time.Duration;


public class MetricsDisplay implements MetricsProcessor {

    private final Output output;
    private final Metrics metrics;
    private final int every;
    private final Duration everyDuration;
    private final TimeSource timeSource;
    private final String name;

    private int index = 0;
    private long lastTime = 0L;

    public MetricsDisplay(final Output output, final Metrics metrics, int every, Duration everyDuration, TimeSource timeSource, String name) {
        this.output = output;
        this.every = every;
        this.metrics = metrics;
        this.everyDuration = everyDuration;
        this.timeSource = timeSource;
        this.name = name;
    }


    @Override
    public void process() {
        index++;
        if (index % every == 0) {
            long currentTime = timeSource.getTime();
            long duration = currentTime - lastTime;

            if (duration > everyDuration.toMillis()) {
                lastTime = currentTime;
                display();
            }
        }
    }

    private void display() {

        output.println("======== Counts for " + name + " =========== ");
        metrics.counters().forEach(counter -> output.println(counter.toString()));
        output.println("======== Gauges for " + name + " =========== ");
        metrics.gauges().forEach(counter -> output.println(counter.toString()));
        output.println("======== Timers for " + name + " =========== ");
        metrics.timers().forEach(counter -> output.println(counter.toString()));
    }
}
