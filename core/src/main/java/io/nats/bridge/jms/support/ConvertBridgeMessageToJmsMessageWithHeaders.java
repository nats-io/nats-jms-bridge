package io.nats.bridge.jms.support;

import io.nats.bridge.messages.BaseMessageWithHeaders;
import io.nats.bridge.messages.Message;
import io.nats.bridge.util.FunctionWithException;
import io.nats.bridge.util.SupplierWithException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.Map;
import java.util.Set;

public class ConvertBridgeMessageToJmsMessageWithHeaders implements FunctionWithException<Message, javax.jms.Message> {

    private final SupplierWithException<BytesMessage> messageCreator;

    public ConvertBridgeMessageToJmsMessageWithHeaders(SupplierWithException<BytesMessage> messageCreator) {
        this.messageCreator = messageCreator;
    }


    public ConvertBridgeMessageToJmsMessageWithHeaders(final Session session) {
        this(new ByteMessageSupplierFromSession(session));
    }

    @Override
    public javax.jms.Message apply(final Message message) throws Exception {
        final BytesMessage bytesMessage = messageCreator.get();

        if (message instanceof BaseMessageWithHeaders) {
            copyHeaders(message, bytesMessage);
        }
        bytesMessage.writeBytes(message.getBodyBytes());
        return bytesMessage;
    }

    private void copyHeaders(Message message, BytesMessage bytesMessage) throws JMSException {
        final Map<String, Object> headers = message.headers();
        final Set<String> keys = headers.keySet();
        for (String key : keys) {
            //ystem.out.println(key);
            bytesMessage.setObjectProperty(key, headers.get(key));
        }


        if (message.timestamp() != -1)
            bytesMessage.setJMSTimestamp(message.timestamp());

        if (message.expirationTime() != -1) {
            //ystem.out.println("ConvertBridgeMessageToJmsMessageWithHeaders EXP TIME " + message.deliveryTime());
            bytesMessage.setJMSExpiration(message.expirationTime());
        }

        if (message.deliveryTime() != -1) {
            //ystem.out.println("ConvertBridgeMessageToJmsMessageWithHeaders DELIVERY TIME " + message.deliveryTime());
            bytesMessage.setJMSDeliveryTime(message.deliveryTime());
        }

        if (message.deliveryMode() != -1)
            bytesMessage.setJMSDeliveryMode(message.deliveryMode());

        if (!Message.NO_TYPE.equals(message.type()))
            bytesMessage.setJMSType(message.type());

        bytesMessage.setJMSRedelivered(message.redelivered());

        if (message.priority() != -1)
            bytesMessage.setJMSPriority(message.priority());

        if (message.correlationID() != null && message.correlationID().trim().length() > 0)
            bytesMessage.setJMSCorrelationID(message.correlationID());

    }
}
