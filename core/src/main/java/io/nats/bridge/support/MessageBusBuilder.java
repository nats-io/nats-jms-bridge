package io.nats.bridge.support;

import io.nats.bridge.MessageBus;

public interface MessageBusBuilder {

    MessageBus build();

    MessageBus build(String destinationOrSubject);
}
