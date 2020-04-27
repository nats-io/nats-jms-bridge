package io.nats.bridge.messages;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        builder.withHeader("header7", (short) 12);
        builder.withHeader("header7", (byte) 12);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }


    @Test
    public void createMessageJustPriority() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("Hello Cruel World".getBytes(StandardCharsets.UTF_8));
        builder.withPriority(1);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }


    @Test
    public void createMessageWithType() {

        long time = System.currentTimeMillis();
        long expirationTime = time + 30_000;

        final MessageBuilder builder = MessageBuilder.builder();
        builder.withPriority(1);
        builder.withExpirationTime(expirationTime).withTimestamp(time);
        builder.withType("TYPE_MESSAGE");
        builder.withCorrelationID("767856");
        builder.withTimestamp(7L);
        builder.withExpirationTime(99L);
        builder.withDeliveryMode(3);
        builder.withDeliveryTime(time + 1);
        builder.withHeader("header1", 700);
        builder.withHeader("header2", false);
        builder.withHeader("header3", 1L);

        StringBuilder builder1 = new StringBuilder();

        for (int index = 0; index < 513; index++) {
            builder1.append('a');
        }
        builder.withHeader("header4", builder1.toString());
        builder.withHeader("header5", 1.1);
        builder.withHeader("header6", 12f);

        builder.withHeader("header7-short", (short) -5);
        builder.withHeader("header8", (byte) 12);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }


    @Test
    public void justShort() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withHeader("header7-short", (short) 500);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void justInt500() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withHeader("header-int", 500);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void justIntNeg() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withHeader("header-int", -1);


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void justString() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withHeader("header-str", "foo");


        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void justBigString() {


        final MessageBuilder builder = MessageBuilder.builder();

        StringBuilder builder1 = new StringBuilder();

        for (int index = 0; index < 513; index++) {
            builder1.append('a');
        }
        builder.withHeader("header-str", builder1.toString());

        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void justByte() {


        final MessageBuilder builder = MessageBuilder.builder();

        builder.withHeader("header-byte", (byte) 10);

        final BaseMessageWithHeaders message1 = (BaseMessageWithHeaders) builder.build();

        final byte[] bytes = message1.getMessageAsBytes();

        final BaseMessageWithHeaders message2 = (BaseMessageWithHeaders) MessageBuilder.builder().buildFromBytes(bytes);


        assertEquals(message1, message2);

    }

    @Test
    public void smallMessage() {


        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("hi");

        Message message = builder.build();

        final byte[] bytes = message.getBodyBytes();

        final Message message2 = MessageBuilder.builder().buildFromBytes(bytes);

        assertTrue(message2 instanceof BytesMessage);
        assertEquals("hi", message2.bodyAsString());

    }

    @Test
    public void bigMessage() {


        final MessageBuilder builder = MessageBuilder.builder();

        StringBuilder builder1 = new StringBuilder();

        for (int index = 0; index < 513; index++) {
            builder1.append('a');
        }
        String longMessage = builder1.toString();
        builder.withHeader("header-str", builder1.toString());
        builder.withBody(longMessage);

        Message message = builder.build();

        final byte[] bytes = message.getBodyBytes();

        final Message message2 = MessageBuilder.builder().buildFromBytes(bytes);

        assertTrue(message2 instanceof BytesMessage);
        assertEquals(longMessage, message2.bodyAsString());

    }
}