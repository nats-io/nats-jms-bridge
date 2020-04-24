package io.nats.bridge.util;

import io.nats.bridge.metrics.Counter;
import org.slf4j.Logger;

import java.util.function.Function;

public class ExceptionHandler {

    private final Logger logger;

    public ExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    public void tryWithLog(final RunnableWithException runnable, final String errorMessage) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            logger.error(errorMessage, ex);
        }
    }

    public void tryWithErrorCount(final RunnableWithException runnable, final Counter errorCounter, final String errorMessage) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            errorCounter.increment();
            logger.error(errorMessage, ex);
        }
    }

    public void tryWithRethrow(final RunnableWithException runnable, final Counter errorCounter,
                               final Function<Exception, RuntimeException> exceptionCreator) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            errorCounter.increment();
            throw exceptionCreator.apply(ex);
        }
    }

    public <T> T tryReturnOrRethrow(final SupplierWithException<T> supplier,
                                    final Function<Exception, RuntimeException> exceptionCreator) {
        try {
            return supplier.get();
        } catch (final Exception ex) {
            throw exceptionCreator.apply(ex);
        }
    }

    public <T, R> R tryFunctionOrRethrow(final T arg, final FunctionWithException<T, R> function,
                                         final Function<Exception, RuntimeException> exceptionCreator) {
        try {
            return function.apply(arg);
        } catch (final Exception ex) {
            throw exceptionCreator.apply(ex);
        }
    }

    public void tryWithRethrow(final RunnableWithException runnable,
                               final Function<Exception, RuntimeException> exceptionCreator) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            throw exceptionCreator.apply(ex);
        }
    }

}
