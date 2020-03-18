package io.nats.bridge;

public interface Message {
    void reply(Message reply);
}
