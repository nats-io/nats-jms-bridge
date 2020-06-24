package io.nats.bridge.messages.transform.transforms;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class SkipTransform implements TransformMessage {

    @Override
    public TransformResult transform(Message inputMessage) {
        return TransformResult.skip();
    }

    @Override
    public String name() {
        return "skip";
    }
}
