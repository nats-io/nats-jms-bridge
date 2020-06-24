package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class ExampleTransform implements TransformMessage {
    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        builder.withHeader("New_Header", "New_Value");
        return TransformResult.success("Added a new header", builder.build());
    }

    @Override
    public String name() {
        return "example";
    }
}
