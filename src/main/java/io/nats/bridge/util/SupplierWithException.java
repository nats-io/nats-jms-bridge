package io.nats.bridge.util;


public interface SupplierWithException<T> {
    T get() throws Exception;
}


