package io.nats.bridge.transform.examples2;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.Configurable;
import io.nats.bridge.messages.transform.TransformResult;

import java.util.Properties;

public class AddHeaderTransform2 implements TransformMessage, Configurable  {

    private Properties properties;

    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        System.out.println("Class AddHeaderTransform: "+ builder.getHeaders());

        final Object rawValue = builder.getHeaders().get(properties.getProperty("H1"));
        final String headerValue = rawValue!= null  ? rawValue.toString() : "";
        System.out.println(headerValue);
        if (headerValue.equals("Hello")) {
            System.out.println("Headers!: "+ builder.getHeaders());
            builder.withHeader(properties.getProperty("H1"), "Hello World");
        } else {
            builder.withHeader( properties.getProperty("H1"), "Say hello please! The world awaits");
        }

        // builder.withReplyHandler(inputMessage::reply); NOTE: initFromMessage does this by default!
        return TransformResult.success("Added a new header", builder.build());
    }

    @Override
    public String name() {
        return "addHeader";
    }

    @Override
    public void configure(final Properties properties) {
        this.properties = properties;
    }
}
