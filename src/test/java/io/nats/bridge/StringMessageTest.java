package io.nats.bridge;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringMessageTest {

    @Test
    public void reply() {
        StringMessage stringMessage = new StringMessage("body");
        stringMessage.reply(null);
    }
}