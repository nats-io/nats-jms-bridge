package io.nats.bridge.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.nats.bridge.messages.Protocol.*;

public class MessageBuilder {
    private final static ObjectMapper mapper = new ObjectMapper();
    static Logger logger = LoggerFactory.getLogger(MessageBuilder.class);
    private long timestamp = -1;
    //TTL plus timestamp
    private long expirationTime = -1;
    //Delivery time is not instant
    private long deliveryTime = -1;
    private int mode = -1;
    private String type = Message.NO_TYPE;
    private boolean redelivered;
    private int priority = -1;
    private Map<String, Object> headers;
    private byte[] body;
    private String correlationID;

    public String getCreator() {
        if (creator==null) {
            return "";
        }
        return creator;
    }

    public MessageBuilder withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    private String creator;

    private Consumer<Message> replyHandler;

    public MessageBuilder() {
    }

    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public MessageBuilder withCorrelationID(String correlationID) {
        this.correlationID = correlationID;
        return this;
    }

    public Consumer<Message> getReplyHandler() {
        if (replyHandler == null) {
            replyHandler = message -> {
                new Exception().fillInStackTrace().printStackTrace();
                System.out.println(creator + "::: DEFAULT HANDLER CALLED " + creator);
            };
        }
        return replyHandler;
    }

    public MessageBuilder withNoReplyHandler(final String description) {
        final Exception caller = new Exception();

        withReplyHandler(message -> {
            System.out.println("NOT EXPECTING A REPLY " + description + "::: creator" +  creator + "::: DEFAULT HANDLER CALLED " + creator);
            System.out.println("CALLER INFO----");
            caller.printStackTrace(System.out);
            new Exception().fillInStackTrace().printStackTrace();
        });
        return this;
    }

    public MessageBuilder withReplyHandler(Consumer<Message> replyHandler) {
        this.replyHandler = replyHandler;
        return this;
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

    public MessageBuilder withDeliveryMode(int mode) {
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
                    isRedelivered(), getPriority(), getCorrelationID(), getHeaders(), getBody(), getReplyHandler());
        }
    }


    public Message buildFromBytes(byte[] buffer) {

        Map<String, Object> headersRead = Collections.emptyMap();

        if (buffer.length > 3) {
            /*
             * Check for protocol markers.
             * If no markers, then no header.
             */
            if (buffer[0] == MARKER_AB &&
                    buffer[1] == MARKER_CD &&
                    buffer[2] == Protocol.MESSAGE_VERSION_MAJOR &&
                    buffer[3] == Protocol.MESSAGE_VERSION_MINOR
            ) {

                final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer));
                try {
                    //Skip markers
                    dataInputStream.skipBytes(4);

                    //Read the header count.
                    int numHHeaders = dataInputStream.readByte();
                    //If there are headers read them.
                    if (numHHeaders > 0) {
                        headersRead = new HashMap<>(numHHeaders); //hold headers read in
                        int headerNameLength; //hold current header name length.
                        byte[] headerNameBytes; //hold header name bytes
                        byte[] stringBytes; //hold bytes to read a string header
                        int stringLength; //hold string length bytes
                        String headerName; //hold the current header name.
                        for (int index = 0; index < numHHeaders; index++) {

                            /*
                             * Read the header name which could be encoded as a header code if under 0.
                             */
                            headerNameLength = dataInputStream.readByte();
                            if (headerNameLength < 0) {
                                headerName = Protocol.getHeaderFromCode(headerNameLength);
                            } else {
                                headerNameBytes = new byte[headerNameLength];
                                dataInputStream.read(headerNameBytes);
                                headerName = new String(headerNameBytes, StandardCharsets.UTF_8);
                            }
                            /*
                             * Read the header type then based on type read the value from the stream.
                             */
                            int type = dataInputStream.readByte();
                            switch (type) {
                                case TYPE_SHORT_STRING:
                                    stringLength = dataInputStream.readByte();
                                    if (stringLength < 1) {
                                        throw new RuntimeException("bad string length");
                                    }
                                    stringBytes = new byte[stringLength];
                                    dataInputStream.read(stringBytes);
                                    headersRead.put(headerName, new String(stringBytes, StandardCharsets.UTF_8));
                                    break;
                                case TYPE_STRING:
                                    stringLength = dataInputStream.readChar();
                                    if (stringLength < 1) {
                                        throw new RuntimeException("bad string length");
                                    }
                                    stringBytes = new byte[stringLength];
                                    dataInputStream.read(stringBytes);
                                    headersRead.put(headerName, new String(stringBytes, StandardCharsets.UTF_8));
                                    break;
                                case TYPE_BOOLEAN_TRUE:
                                    headersRead.put(headerName, true);
                                    break;
                                case TYPE_BOOLEAN_FALSE:
                                    headersRead.put(headerName, false);
                                    break;
//                                case TYPE_UNSIGNED_BYTE: NOT USED YET
//                                    headers.put(headerName, dataInputStream.readUnsignedByte());
//                                    break;
                                case TYPE_BYTE:
                                    headersRead.put(headerName, dataInputStream.readByte());
                                    break;
                                case TYPE_SHORT:
                                    headersRead.put(headerName, dataInputStream.readShort());
                                    break;
//                                case TYPE_UNSIGNED_SHORT: NOT USED YET
//                                    headers.put(headerName, dataInputStream.readUnsignedShort());
//                                    break;
                                case TYPE_INT:
                                    headersRead.put(headerName, dataInputStream.readInt());
                                    break;
                                case TYPE_LONG:
                                    headersRead.put(headerName, dataInputStream.readLong());
                                    break;
                                case TYPE_FLOAT:
                                    headersRead.put(headerName, dataInputStream.readFloat());
                                    break;
                                case TYPE_DOUBLE:
                                    headersRead.put(headerName, dataInputStream.readDouble());
                                    break;
                                default:
                                    if (type < RESERVED_START_TYPES) {
                                        throw new RuntimeException("bad type code");
                                    } else {
                                        headersRead.put(headerName, type);
                                    }
                            }
                        }
                    }
                    //Read the body length, hash, and then read the body.
                    // Check the hash to see if the body gets read correctly from the input stream.
                    final int bodyLength = dataInputStream.readInt();
                    final int bodyHash = dataInputStream.readInt();
                    final byte[] bodyBuffer = new byte[bodyLength];
                    dataInputStream.read(bodyBuffer);
                    if (Protocol.createHashCode(bodyBuffer) != bodyHash) {
                        throw new MessageBuilderException("Body Hash did not match ");
                    }
                    /* This is message builder code so there could be headers that are being injected in already. */
                    if (headers == null) {
                        headers = headersRead;
                    } else {
                        headers.putAll(headersRead);
                    }
                    /* Remove common headers if they were included into the header map, and set the common header property instead. */
                    if (headers.containsKey(HEADER_KEY_TIMESTAMP)) {
                        withTimestamp((long) headers.get(HEADER_KEY_TIMESTAMP));
                        headers.remove(HEADER_KEY_TIMESTAMP);
                    }
                    if (headers.containsKey(HEADER_KEY_EXPIRATION_TIME)) {
                        withExpirationTime((long) headers.get(HEADER_KEY_EXPIRATION_TIME));
                        headers.remove(HEADER_KEY_EXPIRATION_TIME);
                    }
                    if (headers.containsKey(HEADER_KEY_DELIVERY_TIME)) {
                        withDeliveryTime((long) headers.get(HEADER_KEY_DELIVERY_TIME));
                        headers.remove(HEADER_KEY_DELIVERY_TIME);
                    }
                    if (headers.containsKey(HEADER_KEY_MODE)) {
                        withDeliveryMode((int) headers.get(HEADER_KEY_MODE));
                        headers.remove(HEADER_KEY_MODE);
                    }
                    if (headers.containsKey(HEADER_KEY_TYPE)) {
                        withType((String) headers.get(HEADER_KEY_TYPE));
                        headers.remove(HEADER_KEY_TYPE);
                    }
                    if (headers.containsKey(HEADER_KEY_REDELIVERED)) {
                        withRedelivered((boolean) headers.get(HEADER_KEY_REDELIVERED));
                        headers.remove(HEADER_KEY_REDELIVERED);
                    }
                    if (headers.containsKey(HEADER_KEY_PRIORITY)) {
                        withPriority((int) headers.get(HEADER_KEY_PRIORITY));
                        headers.remove(HEADER_KEY_PRIORITY);
                    }

                    /* Set the builder headers and body. */
                    withHeaders(headers);
                    withBody(bodyBuffer);
                    return build();
                } catch (final Exception ex) {
                    /* Exception resort to returning the message as a normal byte buffer message, but it sends a warning to logs. */
                    logger.warn("Unable to parse the message after detecting that headers are present", ex);
                    withBody(buffer);
                    return build();
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

    public MessageBuilder withHeader(final String key, final short value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final byte value) {
        getHeaders().put(key, value);
        return this;
    }

    public MessageBuilder withHeader(final String key, final Object value) {
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
