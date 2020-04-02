package io.nats.bridge;

import io.nats.bridge.messages.StringMessage;
import org.junit.Test;

public class StringMessageTest {

    @Test
    public void reply() {
        StringMessage stringMessage = new StringMessage("body");
        stringMessage.reply(null);
    }
}