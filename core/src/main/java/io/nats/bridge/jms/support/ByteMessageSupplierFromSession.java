package io.nats.bridge.jms.support;

import io.nats.bridge.util.SupplierWithException;

import javax.jms.BytesMessage;
import javax.jms.Session;

public class ByteMessageSupplierFromSession implements SupplierWithException<BytesMessage> {
    private final Session session;

    public ByteMessageSupplierFromSession(Session session) {
        this.session = session;
    }

    @Override
    public BytesMessage get() throws Exception {
        return session.createBytesMessage();
    }
}
