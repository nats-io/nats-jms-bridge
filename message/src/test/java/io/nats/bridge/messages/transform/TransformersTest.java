package io.nats.bridge.messages.transform;


import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TransformersTest {



    class AddHeaderTransform implements TransformMessage {
        @Override
        public TransformResult transform(Message inputMessage) {
            MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
            builder.withHeader("New_Header", "New_Value");
            return TransformResult.success("Added a new header", builder.build());
        }
    }

    class NullTransform implements TransformMessage {
        @Override
        public TransformResult transform(Message inputMessage) {
            return null;
        }
    }

    class ThrowsTransform implements TransformMessage {
        @Override
        public TransformResult transform(Message inputMessage) {
            throw new IllegalStateException("Transform failed");
        }
    }

    class ReturnErrorTransform implements TransformMessage {
        @Override
        public TransformResult transform(Message inputMessage) {
            return TransformResult.error("Unable to transform", new IllegalStateException("Unable to transform"));
        }
    }

    Message createTestMessage() {
        final MessageBuilder builder = MessageBuilder.builder();
        builder.withBody("Hello Mom");
        builder.withHeader("SomeHeader", "SomeValue");
        return builder.build();
    }


    @Test
    public void test () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("addHeader", new AddHeaderTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("addHeader"), testMessage);

        assertEquals(Result.TRANSFORMED, result.getResult());

        assertEquals("Added a new header", result.getStatusMessage().get());

        final Message transformedMessage = result.getTransformedMessage();

        assertEquals("New_Value", transformedMessage.headers().get("New_Header"));
        assertEquals("SomeValue", transformedMessage.headers().get("SomeHeader"));

    }


    @Test
    public void testTransformNotFound () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("addHeader", new AddHeaderTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("foo"), testMessage);
        assertEquals(Result.ERROR, result.getResult());
    }

    @Test
    public void testTransformReturnedNull () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("null", new NullTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("null"), testMessage);
        assertEquals(Result.ERROR, result.getResult());
    }

    @Test
    public void testTransformThrowsException () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("throw", new ThrowsTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("throw"), testMessage);
        assertEquals(Result.ERROR, result.getResult());
    }

    @Test
    public void testTransformReturnsError () {
        final Message testMessage = createTestMessage();
        final Map<String, TransformMessage> transforms = Collections.singletonMap("error", new ReturnErrorTransform());
        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("error"), testMessage);
        assertEquals(Result.ERROR, result.getResult());
    }

    @Test
    public void testLoadRunNoOp () {
        final Message testMessage = createTestMessage();

        final Map<String, TransformMessage> transforms = Transformers.loadTransforms();

        final TransformResult result = Transformers.runTransforms(transforms, Collections.singletonList("noop"), testMessage);


        assertEquals(Result.TRANSFORMED, result.getResult());


        final Message transformedMessage = result.getTransformedMessage();

        assertEquals("SomeValue", transformedMessage.headers().get("SomeHeader"));
    }
}