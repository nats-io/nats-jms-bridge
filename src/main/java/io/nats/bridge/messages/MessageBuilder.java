package io.nats.bridge.messages;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.nats.bridge.messages.Protocol.*;

public class MessageBuilder {

    private long timestamp=-1;
    //TTL plus timestamp
    private long expirationTime=-1;
    //Delivery time is not instant
    private long deliveryTime=-1;
    private int mode=-1;
    private String type = Message.NO_TYPE;
    private boolean redelivered;
    private int priority=-1;
    private Map<String, Object> headers;
    private byte[] body;
    private final static ObjectMapper mapper = new ObjectMapper();

    private  Consumer<Message> replyHandler;

    public Consumer<Message> getReplyHandler() {
        if (replyHandler == null) {
            replyHandler = message -> System.out.println("DEFAULT HANDLER CALLED ");
        }
        return replyHandler;
    }

    public MessageBuilder withReplyHandler(Consumer<Message> replyHandler) {
        this.replyHandler = replyHandler;
        return this;
    }

    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

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
        if (headers == null) {
            headers = new HashMap<>(9);
        }
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

    public MessageBuilder withBody(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public Message build() {
        if (timestamp == -1 && headers == null && deliveryTime == -1 && expirationTime == -1 && mode == -1
                && type == Message.NO_TYPE && !redelivered && priority == -1) {
            return new BaseMessage(getBody(), getReplyHandler());
        } else {
            return new BaseMessageWithHeaders(getTimestamp(), getExpirationTime(), getDeliveryTime(), getMode(), getType(),
                    isRedelivered(), getPriority(), getHeaders(), getBody(), getReplyHandler());
        }
    }


    public Message buildFromBytes(byte[] buffer) {

        if (buffer.length > 5) {

            if (buffer[0] == MARKER_AB &&
                    buffer[1] == MARKER_CD &&
                    buffer[2] == Protocol.MESSAGE_VERSION_MAJOR &&
                    buffer[3] == Protocol.MESSAGE_VERSION_MINOR &&
                    buffer[4] == MARKER_AB &&
                    buffer[5] == MARKER_CD
            ) {

                final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer));
                try {
                    dataInputStream.skipBytes(6);

                    //Read the header
                    final int jsonLength = dataInputStream.readInt();
                    final int jsonHash = dataInputStream.readInt();
                    final byte[] jsonByteBuffer = new byte[jsonLength];
                    dataInputStream.read(jsonByteBuffer);
                    if (Protocol.createHashCode(jsonByteBuffer) != jsonHash) {
                        throw new MessageBuilderException("JSON Hash did not match for headers");
                    }
                    final Map<String, Object> header = mapper.readValue(jsonByteBuffer, Map.class);

                    //Read the body
                    final int bodyLength = dataInputStream.readInt();

                    //ystem.out.println("body len " + bodyLength);
                    final int bodyHash = dataInputStream.readInt();
                    //ystem.out.println("body hash " + bodyHash);
                    final byte[] bodyBuffer = new byte[bodyLength];
                    dataInputStream.read(bodyBuffer);
                    if (Protocol.createHashCode(bodyBuffer) != bodyHash) {
                        throw new MessageBuilderException("Body Hash did not match ");
                    }

                    /* read headers */
                    if (header.containsKey(HEADER_KEY_TIMESTAMP)) {
                        withTimestamp((long) header.get(HEADER_KEY_TIMESTAMP));
                        header.remove(HEADER_KEY_TIMESTAMP);
                    }
                    if (header.containsKey(HEADER_KEY_EXPIRATION_TIME)) {
                        withExpirationTime((long) header.get(HEADER_KEY_EXPIRATION_TIME));
                        header.remove(HEADER_KEY_EXPIRATION_TIME);
                    }
                    if (header.containsKey(HEADER_KEY_DELIVERY_TIME)) {
                        withDeliveryTime((long) header.get(HEADER_KEY_DELIVERY_TIME));
                        header.remove(HEADER_KEY_DELIVERY_TIME);
                    }
                    if (header.containsKey(HEADER_KEY_MODE)) {
                        withMode((int) header.get(HEADER_KEY_MODE));
                        header.remove(HEADER_KEY_MODE);
                    }
                    if (header.containsKey(HEADER_KEY_TYPE)) {
                        withType((String) header.get(HEADER_KEY_TYPE));
                        header.remove(HEADER_KEY_TYPE);
                    }
                    if (header.containsKey(HEADER_KEY_REDELIVERED)) {
                        withRedelivered((boolean) header.get(HEADER_KEY_REDELIVERED));
                        header.remove(HEADER_KEY_REDELIVERED);
                    }
                    if (header.containsKey(HEADER_KEY_PRIORITY)) {
                        withPriority((int) header.get(HEADER_KEY_PRIORITY));
                        header.remove(HEADER_KEY_PRIORITY);
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

    public MessageBuilder withHeader(final String key, final int value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final long value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final boolean value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final double value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final float value) {
        getHeaders().put(key, value);
        return this;
    }


    public MessageBuilder withHeader(final String key, final String value) {
        getHeaders().put(key, value);
        return this;
    }

}
