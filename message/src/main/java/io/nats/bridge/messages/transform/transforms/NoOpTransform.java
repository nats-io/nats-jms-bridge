package io.nats.bridge.messages.transform.transforms;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class NoOpTransform implements TransformMessage {

    @Override
    public TransformResult transform(final Message inputMessage) {
        return TransformResult.success(inputMessage);
    }

    @Override
    public String name() {
        return "noop";
    }
}
