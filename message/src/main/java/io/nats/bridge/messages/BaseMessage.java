package io.nats.bridge.messages;

import java.util.function.Consumer;

public class BaseMessage implements BytesMessage {

    private final byte[] bytes;

    private final Consumer<Message> replyHandler;

    public BaseMessage(byte[] bytes, Consumer<Message> replyHandler) {
        this.bytes = bytes;
        this.replyHandler = replyHandler;
    }

    @Override
    public void reply(final Message reply) {
        replyHandler.accept(reply);
    }

    public byte[] getBodyBytes() {
        return bytes;
    }
}
