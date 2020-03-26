package io.nats.bridge.metrics;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import org.junit.Test;

import java.time.Duration;

public class MetricsDisplayTest {

    @Test
    public void process() {

        SimpleMetrics metrics = new SimpleMetrics(new TimeSource() {
            @Override
            public long getTime() {
               return System.currentTimeMillis();
            }
        });

        metrics.createCounter("count").increment();
        metrics.createCounter("count2").recordCount(10);
        metrics.createGauge("gauge").recordLevel(10);
        metrics.createTimeTracker("timer").recordTiming(10);
        metrics.createTimeTracker("timer2").recordTiming(() -> {
        });




        Output output = new Output() {
            @Override
            public void println(String message) {
                System.out.println(message);
            }
        };

        MetricsDisplay display = new MetricsDisplay(output, metrics, 2, Duration.ZERO, System::currentTimeMillis);

        display.process();
        display.process();
    }
}