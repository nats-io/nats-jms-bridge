package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class ModifyBodyTransform implements TransformMessage {


    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        System.out.println("Class ModifyBodyTransform: "+ inputMessage.bodyAsString());

        builder.withBody(inputMessage.bodyAsString() + " FROM BRAZIL!");

        return TransformResult.success("Message came from Brazil", builder.build());
    }

    @Override
    public String name() {
        return "changeBody";
    }
}
