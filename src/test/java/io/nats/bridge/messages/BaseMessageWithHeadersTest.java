package io.nats.bridge.messages;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BaseMessageWithHeadersTest {

    @Test
    public void createMessage() {

        long time = System.currentTimeMillis();
        long expirationTime = time + 30_000;

        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("Hello Cruel World".getBytes(StandardCharsets.UTF_8));
        builder.withPriority(1);
        builder.withExpirationTime(expirationTime).withTimestamp(time);
        builder.withDeliveryMode(3);
        builder.withRedelivered(true);
        builder.withDeliveryTime(time + 1);
        builder.withHeader("header1", 1);
        builder.withHeader("header2", true);
        builder.withHeader("header3", 1L);
        builder.withHeader("header4", "hello");
        builder.withHeader("header5", 1.1);
        builder.withHeader("header6", 12f);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();


        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders)MessageBuilder.builder().buildFromBytes(message1.getMessageAsBytes());


        assertEquals(message1, message2);

    }
}