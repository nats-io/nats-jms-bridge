package io.nats.bridge.util;

import javax.jms.JMSException;
import javax.naming.NamingException;

public interface SupplierWithException<T> {
    T get() throws JMSException, InterruptedException, NamingException;
}


