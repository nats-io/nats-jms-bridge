package io.nats.bridge.metrics;

import io.nats.bridge.metrics.implementation.SimpleMetrics;
import org.junit.Test;

import java.time.Duration;

public class MetricsDisplayTest {

    @Test
    public void process() {

        final SimpleMetrics metrics = new SimpleMetrics(System::currentTimeMillis);

        metrics.createCounter("count").increment();
        metrics.createCounter("count2").recordCount(10);
        metrics.createGauge("gauge").recordLevel(10);
        metrics.createTimeTracker("timer").recordTiming(10);
        metrics.createTimeTracker("timer2").recordTiming(() -> {
        });


        final Output output = new Output() {
            @Override
            public void println(String message) {
                System.out.println(message);
            }
        };

        final MetricsDisplay display = new MetricsDisplay(output, metrics, 2, Duration.ZERO, System::currentTimeMillis, "foo");

        display.process();
        display.process();
    }
}