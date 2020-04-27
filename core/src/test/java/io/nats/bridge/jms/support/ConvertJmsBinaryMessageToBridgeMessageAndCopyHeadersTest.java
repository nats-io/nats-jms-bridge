package io.nats.bridge.jms.support;

import io.nats.bridge.mock.JMSBinaryMessage;
import io.nats.bridge.mock.JMSNoMessage;
import io.nats.bridge.mock.JMSTextMessage;
import io.nats.bridge.util.FunctionWithException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.junit.Assert.*;

public class ConvertJmsBinaryMessageToBridgeMessageAndCopyHeadersTest {

    private JMSMessageBusBuilder builder;
    private javax.jms.Message testMessage;
    private FunctionWithException<Message, io.nats.bridge.messages.Message> messageConverter;
    private io.nats.bridge.messages.Message bridgeMessage;

    @Before
    public void setUp() throws Exception {
        builder = JMSMessageBusBuilder.builder();
        messageConverter = builder.withCopyHeaders(true).getJmsMessageConverter();
        testMessage = new JMSBinaryMessage("Hello Mom");
        initTestMessage();
    }

    private void initTestMessage() throws JMSException {

        testMessage.setJMSCorrelationID("foo");
        testMessage.setJMSPriority(1);
        testMessage.setJMSType("bar");
        testMessage.setJMSExpiration(2L);
        testMessage.setJMSDeliveryTime(3L);
        testMessage.setJMSRedelivered(true);
        testMessage.setJMSTimestamp(4L);
        testMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);

        try {
            testMessage.setBooleanProperty("someprop", true);
        } catch (JMSException e) {
            fail();
        }
    }


    private void validateAll() throws JMSException {
        assertNotNull(bridgeMessage);
        assertTrue(bridgeMessage instanceof io.nats.bridge.messages.BaseMessageWithHeaders);

        //assertEquals("foo", bridgeMessage.correlationID());
        assertEquals(1, bridgeMessage.priority());
        assertEquals("bar", bridgeMessage.type());
        assertEquals(2L, bridgeMessage.expirationTime());
        assertEquals(3L, bridgeMessage.deliveryTime());
        assertEquals(DeliveryMode.NON_PERSISTENT, bridgeMessage.deliveryMode());
        assertEquals(4L, bridgeMessage.timestamp());

        //assertEquals(testMessage.getJMSCorrelationID(), bridgeMessage.correlationID());
        assertEquals(testMessage.getJMSPriority(), bridgeMessage.priority());
        assertEquals(testMessage.getJMSType(), bridgeMessage.type());
        assertEquals(testMessage.getJMSExpiration(), bridgeMessage.expirationTime());
        assertEquals(testMessage.getJMSDeliveryTime(), bridgeMessage.deliveryTime());
        assertEquals(testMessage.getJMSDeliveryMode(), bridgeMessage.deliveryMode());
        assertEquals(testMessage.getJMSTimestamp(), bridgeMessage.timestamp());
        assertEquals(true, bridgeMessage.headers().get("someprop"));
    }

    @Test
    public void apply() throws Exception {


        testMessage.setJMSReplyTo(new Destination() {
        });
        bridgeMessage = messageConverter.apply(testMessage);
        validateAll();


    }

    @Test
    public void applyWithReplyTo() throws Exception {

        bridgeMessage = messageConverter.apply(testMessage);
        validateAll();


    }

    @Test
    public void applyText() throws Exception {

        testMessage = new JMSTextMessage("Hello");
        initTestMessage();

        testMessage.setJMSReplyTo(new Destination() {
        });
        bridgeMessage = messageConverter.apply(testMessage);
        validateAll();


    }

    @Test
    public void applyTextWithReplyTo() throws Exception {

        testMessage = new JMSTextMessage("Hello");
        initTestMessage();

        bridgeMessage = messageConverter.apply(testMessage);
        validateAll();


    }

    @Test
    public void illegalMessageType() throws Exception {

        try {
            testMessage = new JMSNoMessage();
            initTestMessage();

            bridgeMessage = messageConverter.apply(testMessage);
            fail();
        } catch (Exception ex) {
            assertTrue(true);
        }


    }

}