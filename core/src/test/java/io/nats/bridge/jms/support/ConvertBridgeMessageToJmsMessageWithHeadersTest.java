package io.nats.bridge.jms.support;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.mock.jms.JMSBinaryMessage;
import io.nats.bridge.util.SupplierWithException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConvertBridgeMessageToJmsMessageWithHeadersTest {

    private MessageBuilder messageBuilder;
    private Message messageBusMessage;
    private javax.jms.BytesMessage testJMSMessage;
    private ConvertBridgeMessageToJmsMessageWithHeaders messageConverter;
    private io.nats.bridge.messages.Message bridgeMessage;

    @Before
    public void setUp() throws Exception {


        testJMSMessage = new JMSBinaryMessage("Hello Mom");

        messageConverter = new ConvertBridgeMessageToJmsMessageWithHeaders(new SupplierWithException<BytesMessage>() {
            @Override
            public BytesMessage get() throws Exception {
                return testJMSMessage;
            }
        });

        messageBuilder = MessageBuilder.builder();
        initTestMessage();
        messageBusMessage = messageBuilder.build();


    }


    private void initTestMessage() throws JMSException {


        messageBuilder.withDeliveryMode(1);
        messageBuilder.withCorrelationID("foo");
        messageBuilder.withPriority(1);
        messageBuilder.withExpirationTime(2L);
        messageBuilder.withDeliveryTime(3L);
        messageBuilder.withRedelivered(true);
        messageBuilder.withTimestamp(4L);
        messageBuilder.withHeader("someprop", true);
        messageBuilder.withHeader("replyTo", "somequeuename");

    }

    @Test
    public void test() throws Exception {
        javax.jms.Message message = messageConverter.apply(messageBusMessage);
        assertEquals("foo", message.getJMSCorrelationID());
        assertEquals(1, message.getJMSPriority());
        assertEquals(2L, message.getJMSExpiration());
        assertEquals(3L, message.getJMSDeliveryTime());
        assertTrue(message.getJMSRedelivered());
        assertEquals(4L, message.getJMSTimestamp());
        assertTrue(message.getBooleanProperty("someprop"));
        assertEquals("somequeuename", message.getStringProperty("replyTo"));
    }


}