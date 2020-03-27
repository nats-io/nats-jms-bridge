package io.nats.bridge.util;

import io.nats.bridge.TimeSource;
import io.nats.bridge.jms.JMSMessageBusException;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import io.nats.bridge.nats.NatsMessageBusException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionHandlerTest {

    final ExceptionHandler exceptionHandler = new ExceptionHandler();


    @Test
    public void tryWithLog() {
        exceptionHandler.tryWithLog(() -> {
            throw new JMSMessageBusException("Bus exception");
        }, "error");
    }

    @Test
    public void tryWithErrorCount() {

        final Metrics metrics = new SimpleMetrics(System::currentTimeMillis);
        final Counter count = metrics.createCounter("count");

        exceptionHandler.tryWithErrorCount(() -> {
            throw new NatsMessageBusException("Nats error");
        }, count, "error");

       assertEquals (1L, ((GetMetric)count).getValue());
    }

    @Test
    public void tryWithRethrow() {

    }

    @Test
    public void tryReturnOrRethrow() {
    }

    @Test
    public void tryFunctionOrRethrow() {
    }

    @Test
    public void testTryWithRethrow() {
    }
}