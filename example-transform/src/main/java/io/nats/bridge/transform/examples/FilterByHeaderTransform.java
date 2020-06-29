package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

class FilterByHeaderTransform implements TransformMessage {
    @Override
    public TransformResult transform(final Message inputMessage) {

        System.out.println("Class FilterByHeaderTransform: " + inputMessage.headers());
        final String headerValue = inputMessage.headers().get("H1").toString();
        System.out.println(headerValue);

        //Example of a filter message based on a header.
        if (headerValue.equals("Hello")) {
            return TransformResult.skip("H1 was hello so do not send this message");
            //Example of conditionally modifying a message.
        } else if (headerValue.equals("Goodbye")) {
            final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
            final String newBody = inputMessage.bodyAsString() + " Goodbye";
            builder.withBody(newBody);
            return TransformResult.success("Changed body bc header was Goodbye", builder.build());

            //Example of conditionally not transforming message.
        } else if (headerValue.equals("Don't Touch")) {
            return TransformResult.notTransformed("Did not transform the message bc it said don't touch");
        } else{
            //Example did not really change.
            return TransformResult.notTransformed("Added a new header");
        }
    }

    @Override
    public String name() {
        return "all";
    }
}
