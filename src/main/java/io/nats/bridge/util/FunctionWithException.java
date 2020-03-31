package io.nats.bridge.util;

public interface FunctionWithException<T, R> {

    R apply(T var1) throws Exception;

}
