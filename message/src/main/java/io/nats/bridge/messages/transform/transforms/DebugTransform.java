package io.nats.bridge.messages.transform.transforms;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class DebugTransform implements TransformMessage {
    @Override
    public TransformResult transform(Message inputMessage) {

        System.out.println("DEBUG TRANSFORMED CALLED");
        final TransformResult result = TransformResult.success(inputMessage);
        System.out.println("DEBUG TRANSFORMED DONE");
        return result;

    }

    @Override
    public String name() {
        return "debug";
    }
}
