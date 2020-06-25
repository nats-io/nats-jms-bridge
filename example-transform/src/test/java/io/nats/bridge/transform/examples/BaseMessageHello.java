package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.BaseMessageWithHeaders;
import io.nats.bridge.messages.MessageBuilder;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class BaseMessageHello {

    @Test
    public void simpleMessage(){

        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("Hello World Robert".getBytes(StandardCharsets.UTF_8));
        builder.withHeader("Hello", true);

        final BaseMessageWithHeaders messageHello = (BaseMessageWithHeaders) builder.build();

    }
}
