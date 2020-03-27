package io.nats.bridge.util;

import io.nats.bridge.metrics.Counter;

import java.util.function.Function;



public class ExceptionHandler {
    public void tryWithLog(final RunnableWithException runnable, final String errorMessage) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            ex.printStackTrace();
            System.out.println(errorMessage);
        }
    }

    public void tryWithErrorCount(final RunnableWithException runnable, final Counter errorCounter, final String errorMessage) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            errorCounter.increment();
            //TODO add logging
            ex.printStackTrace();
            System.out.println(errorMessage);
        }
    }

    public void tryWithRethrow(final RunnableWithException runnable, final Counter errorCounter,
                               final Function<Exception, Exception> exceptionCreator) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            errorCounter.increment();
            exceptionCreator.apply(ex);
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

    public <T,R> R tryFunctionOrRethrow(final T arg, final FunctionWithException<T, R> function,
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
