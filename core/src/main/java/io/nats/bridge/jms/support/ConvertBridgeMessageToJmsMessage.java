package io.nats.bridge.jms.support;

import io.nats.bridge.messages.Message;
import io.nats.bridge.util.FunctionWithException;

import javax.jms.BytesMessage;
import javax.jms.Session;

public class ConvertBridgeMessageToJmsMessage implements FunctionWithException<Message, javax.jms.Message> {

    private final Session session;

    public ConvertBridgeMessageToJmsMessage(final Session session) {
        this.session = session;
    }

    @Override
    public javax.jms.Message apply(final Message message) throws Exception {
        final BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(message.getMessageBytes());
        return bytesMessage;
    }
}
