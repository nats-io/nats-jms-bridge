package io.nats.bridge.messages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageTest {

    @Test
    public void timestamp() {

        final Message message = new Message() {
        };

        assertEquals(-1, message.timestamp());
        assertEquals(-1, message.deliveryTime());
        assertEquals(-1, message.priority());
        assertFalse(message.redelivered());
        assertEquals("NO_TYPE", message.type());
        assertEquals(-1, message.expirationTime());

        assertEquals(-1, message.deliveryMode());

        assertEquals(0, message.getBodyBytes().length);


    }
}