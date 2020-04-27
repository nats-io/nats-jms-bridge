package io.nats.bridge.util;

import io.nats.bridge.jms.JMSMessageBusException;
import io.nats.bridge.jms.support.JMSMessageBusBuilderException;
import io.nats.bridge.metrics.Counter;
import io.nats.bridge.metrics.GetMetric;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import io.nats.bridge.nats.NatsMessageBusException;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExceptionHandlerTest {

    final ExceptionHandler exceptionHandler = new ExceptionHandler(LoggerFactory.getLogger(ExceptionHandlerTest.class));


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

        assertEquals(1L, ((GetMetric) count).getValue());
    }

    @Test
    public void tryWithRethrow() {
        try {
            exceptionHandler.tryWithRethrow(() -> {
                throw new Exception();
            }, e -> new NatsMessageBusException("error", e));
            fail();
        } catch (NatsMessageBusException ex) {
            assertEquals("error", ex.getMessage());
        }
    }

    @Test
    public void tryReturnOrRethrow() {
        try {
            exceptionHandler.tryReturnOrRethrow((SupplierWithException<String>) () -> {
                throw new Exception("test");
            }, e -> new JMSMessageBusException("error", e));
            fail();
        } catch (JMSMessageBusException ex) {
            assertEquals("error", ex.getMessage());
        }
    }

    @Test
    public void tryFunctionOrRethrow() {
        try {
            exceptionHandler.tryFunctionOrRethrow("hi", (String var) -> {
                throw new Exception("test");
            }, e -> new JMSMessageBusException("error", e));
            fail();
        } catch (JMSMessageBusException ex) {
            assertEquals("error", ex.getMessage());
        }
    }

    @Test
    public void testTryWithRethrow() {
        final Metrics metrics = new SimpleMetrics(System::currentTimeMillis);
        final Counter count = metrics.createCounter("count");

        try {
            exceptionHandler.tryWithRethrow(() -> {
                throw new Exception();
            }, count, e -> new NatsMessageBusException("error", e));
            fail();
        } catch (NatsMessageBusException ex) {
            assertEquals("error", ex.getMessage());
            new JMSMessageBusBuilderException(ex.getMessage(), ex);
        }
        assertEquals(1L, ((GetMetric) count).getValue());
    }
}