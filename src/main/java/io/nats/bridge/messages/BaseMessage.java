package io.nats.bridge.messages;

public class BaseMessage implements BytesMessage {

    private final byte [] bytes;

    public BaseMessage(byte[] bytes) {
        this.bytes = bytes;
    }

    public  byte [] getBodyBytes() {
        return bytes;
    }
}
