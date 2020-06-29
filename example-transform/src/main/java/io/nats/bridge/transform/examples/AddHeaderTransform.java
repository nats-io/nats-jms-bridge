package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

import java.util.function.Consumer;

public class AddHeaderTransform implements TransformMessage {


    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        System.out.println("Class AddHeaderTransform: "+ builder.getHeaders());

        final Object rawValue = builder.getHeaders().get("H1");
        final String headerValue = rawValue!= null  ? rawValue.toString() : "";
        System.out.println(headerValue);
        if (headerValue.equals("Hello")) {
            System.out.println("Headers!: "+ builder.getHeaders());
            builder.withHeader("H1", "Hello World");
        } else {
            builder.withHeader("H1", "Say hello please! The world awaits");
        }

        // builder.withReplyHandler(inputMessage::reply); NOTE: initFromMessage does this by default!
        return TransformResult.success("Added a new header", builder.build());
    }

    @Override
    public String name() {
        return "addHeader";
    }
}
