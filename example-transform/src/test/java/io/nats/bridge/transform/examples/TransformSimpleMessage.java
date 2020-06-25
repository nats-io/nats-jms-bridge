package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.Result;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;
import io.nats.bridge.messages.transform.Transformers;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TransformSimpleMessage {

    Message createTestMessage() {
        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("Hello World!");
        builder.withHeader("H1", "Hello");
        return builder.build();
    }

    @Test
    public void testTransform () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("H1", new AddHeaderTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("H1"), testMessage);

        System.out.println(testMessage);
        System.out.println(testMessage.bodyAsString());
        System.out.println(transforms);

        System.out.println(Result.TRANSFORMED);
        System.out.println(result.getResult());
        assertEquals(Result.TRANSFORMED, result.getResult());

        assertEquals("Added a new header", result.getStatusMessage().get());

        final Message transformedMessage = result.getTransformedMessage();

        assertEquals("Hello World", transformedMessage.headers().get("H1"));
        System.out.println(transformedMessage.headers().toString());
    }



}
