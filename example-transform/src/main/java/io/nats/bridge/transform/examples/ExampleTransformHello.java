package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class ExampleTransformHello implements TransformMessage {


    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);

        if (builder.getHeaders().containsValue("Hello")) {
            builder.withHeader(builder.getHeaders().values() + " World!!!", true);
        }

        return TransformResult.success("Added a new header", builder.build());
    }


    @Override
    public String name() {
        return "hello";
    }

}
