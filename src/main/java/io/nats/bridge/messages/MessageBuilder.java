package io.nats.bridge.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class MessageBuilder {

    private long timestamp;
    //TTL plus timestamp
    private long expirationTime;
    //Delivery time is not instant
    private long deliveryTime;
    private int mode;
    private String type = Message.NO_TYPE;
    private boolean redelivered;
    private int priority;
    private Map<String, Object> headers;
    private byte[] body;

    public HeaderFactory getHeaderFactory() {
        if (headerFactory == null) {
            headerFactory = bytes -> Collections.emptyMap();
        }
        return headerFactory;
    }

    public MessageBuilder setHeaderFactory(HeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
        return this;
    }

    private HeaderFactory headerFactory;

    public long getTimestamp() {
        return timestamp;
    }

    public MessageBuilder withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public MessageBuilder withExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    public long getDeliveryTime() {
        return deliveryTime;
    }

    public MessageBuilder withDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
        return this;
    }

    public int getMode() {
        return mode;
    }

    public MessageBuilder withMode(int mode) {
        this.mode = mode;
        return this;
    }

    public String getType() {
        return type;
    }

    public MessageBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public MessageBuilder withRedelivered(boolean redelivered) {
        this.redelivered = redelivered;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public MessageBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public MessageBuilder withHeaders(Map<String, Object> headers) {
        this.headers = headers;
        return this;
    }

    public byte[] getBody() {
        return body;
    }

    public MessageBuilder withBody(byte[] body) {
        this.body = body;
        return this;
    }

    public Message build() {
        if (timestamp == -1 && headers == null && deliveryTime == -1 && expirationTime == -1
                && type == Message.NO_TYPE && !redelivered && priority == -1) {
            return new BaseMessage(body);
        } else {
            return new BaseMessageWithHeaders(timestamp, expirationTime, deliveryTime, mode, type,
                    redelivered, priority, headers, body);
        }
    }


    public static int hashCode(byte[] value) {
        int h = 0;
        byte[] var2 = value;
        int var3 = value.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            byte v = var2[var4];
            h = 31 * h + (v & 255);
        }

        return h;
    }

    private final byte magicByte1 = 20;
    private final byte magicByte2 = 20;
    private final byte magicByte3 = (byte) 0xbe;
    private final byte magicByte4 = (byte) 0xef;


    //<magic number><version_number><json_len><json_hash><json_flat_map><payload len><payload_hash><payload>
    public Message buildFromBytes(byte[] buffer) {

        if (buffer.length > 5) {

            if (buffer[0] == magicByte1 &&
                    buffer[1] == magicByte2 &&
                    buffer[2] == magicByte3 &&
                    buffer[3] == magicByte4) {

                final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer));
                try {

                    //Skip magic byte
                    dataInputStream.skipBytes(4);

                    // Check the version
                    final int version = dataInputStream.readInt();

                    if (version != 1) {
                        throw new IllegalStateException("Unsupported version " + version);
                    }

                    //Read the header
                    final int jsonLength = dataInputStream.readInt();
                    final int jsonHash = dataInputStream.readInt();
                    final byte[] jsonByteBuffer = new byte[jsonLength];
                    dataInputStream.read(jsonByteBuffer);
                    if (hashCode(jsonByteBuffer) != jsonHash) {
                        throw new MessageBuilderException("JSON Hash did not match ");
                    }
                    final Map<String, Object> header = getHeaderFactory().readHeader(jsonByteBuffer);

                    //Read the body
                    final int bodyLength = dataInputStream.readInt();
                    final int bodyHash = dataInputStream.readInt();
                    final byte[] bodyBuffer = new byte[bodyLength];
                    if (hashCode(bodyBuffer) != bodyHash) {
                        throw new MessageBuilderException("Body Hash did not match ");
                    }

                    /* read headers */
                    if (header.containsKey("timestamp")) {
                        withTimestamp((long) header.get("timestamp"));
                    }
                    if (header.containsKey("expirationTime")) {
                        withExpirationTime((long) header.get("expirationTime"));
                    }
                    if (header.containsKey("deliveryTime")) {
                        withDeliveryTime((long) header.get("deliveryTime"));
                    }
                    if (header.containsKey("deliveryTime")) {
                        withDeliveryTime((long) header.get("deliveryTime"));
                    }
                    if (header.containsKey("mode")) {
                        withMode((int) header.get("mode"));
                    }
                    if (header.containsKey("type")) {
                        withType((String) header.get("type"));
                    }
                    if (header.containsKey("redelivered")) {
                        withRedelivered((boolean) header.get("redelivered"));
                    }
                    if (header.containsKey("priority")) {
                        withPriority((int) header.get("priority"));
                    }

                    withHeaders(header);
                    withBody(bodyBuffer);

                    return build();

                } catch (final IOException ex) {
                    throw new MessageBuilderException("Unable to create message", ex);
                }

            } else {
                withBody(buffer);
                return build();
            }
        } else {
            withBody(buffer);
            return build();
        }

    }
}
