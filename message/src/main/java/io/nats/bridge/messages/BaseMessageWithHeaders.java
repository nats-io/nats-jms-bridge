package io.nats.bridge.messages;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.nats.bridge.messages.Protocol.*;

public class BaseMessageWithHeaders implements BytesMessage {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final long timestamp;
    //TTL plus timestamp
    private final long expirationTime;
    //Delivery time is not instant
    private final long deliveryTime;
    private final int mode;
    private final String type;
    private final boolean redelivered;
    private final int priority;
    private final String correlationID;
    private final Map<String, Object> headers;
    private final byte[] bodyBytes;
    private final Consumer<Message> replyHandler;


    public BaseMessageWithHeaders(long timestamp, long expirationTime, long deliveryTime, int mode, String type,
                                  boolean redelivered, int priority, String correlationID, Map<String, Object> headers, byte[] bytes,
                                  Consumer<Message> replyHandler) {
        this.timestamp = timestamp;
        this.expirationTime = expirationTime;
        this.deliveryTime = deliveryTime;
        this.mode = mode;
        this.type = type;
        this.redelivered = redelivered;
        this.priority = priority;
        this.correlationID = correlationID;
        this.headers = headers;
        this.bodyBytes = bytes == null ? new byte[0] : bytes;
        this.replyHandler = replyHandler;
    }

    @Override
    public String correlationID() {
        return correlationID;
    }

    @Override
    public void reply(final Message reply) {
        replyHandler.accept(reply);
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public long timestamp() {
        return timestamp;
    }

    //TTL plus timestamp
    public long expirationTime() {
        return expirationTime;
    }

    //Delivery time is not instant
    public long deliveryTime() {
        return deliveryTime;
    }

    public int deliveryMode() {
        return mode;
    }

    public String type() {
        return type;
    }

    public boolean redelivered() {
        return redelivered;
    }

    public int priority() {
        return priority;
    }

    public Map<String, Object> headers() {
        if (headers == null) return Collections.emptyMap();
        return headers;
    }

    /**
     * Convert byte representation of Message
     *
     * @return bytes of message
     */
    public byte[] getMessageAsBytes() {
        final ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        final DataOutputStream streamOut = new DataOutputStream(bytesOutputStream);
        try {
            /*
             * Write the markers and message protocol version.
             */
            streamOut.writeByte(Protocol.MARKER_AB);
            streamOut.writeByte(Protocol.MARKER_CD);
            streamOut.writeByte(Protocol.MESSAGE_VERSION_MAJOR);
            streamOut.writeByte(Protocol.MESSAGE_VERSION_MINOR);
            /*
             * Create the map that holds the headers.
             */
            final Map<String, Object> outputHeaders = (headers == null) ? Collections.emptyMap() : headers;
            /*
                The size of headers we are sending which is the baked headers plus user headers.
             */
            int headerSize = 0;
            /*
                Only send these common headers if not equal to the default value.
            */
            if (deliveryTime > 0) headerSize++;
            if (mode != -1) headerSize++;
            if (expirationTime > 0) headerSize++;
            if (timestamp > 0) headerSize++;
            if (type != null && !NO_TYPE.equals(type)) headerSize++;
            if (priority != -1) headerSize++;
            if (redelivered) headerSize++;

            headerSize += outputHeaders.size();
            streamOut.writeByte(headerSize);

            /* Limit headers we are sending to the size of 1 unsigned byte. */
            if (headerSize > 255)
                throw new MessageException("Can't write out the message as there are too many headers");

            if (deliveryTime() > 0) {
                streamOut.writeByte(HEADER_KEY_DELIVERY_TIME_CODE);
                streamOut.writeByte(TYPE_LONG);
                streamOut.writeLong(deliveryTime());
            }
            if (deliveryMode() != -1) {
                streamOut.writeByte(HEADER_KEY_MODE_CODE);
                streamOut.writeByte(deliveryMode());
            }
            if (expirationTime() > 0) {
                streamOut.writeByte(HEADER_KEY_EXPIRATION_TIME_CODE);
                streamOut.writeByte(TYPE_LONG);
                streamOut.writeLong(expirationTime());
            }
            if (timestamp() > 0) {
                streamOut.writeByte(HEADER_KEY_TIMESTAMP_CODE);
                streamOut.writeByte(TYPE_LONG);
                streamOut.writeLong(timestamp());
            }
            if (type() != null && !NO_TYPE.equals(type())) {
                streamOut.writeByte(HEADER_KEY_TYPE_CODE);
                streamOut.writeByte(TYPE_SHORT_STRING);
                streamOut.writeByte(type().length());
                streamOut.write(type().getBytes(StandardCharsets.UTF_8));
            }
            if (priority != -1) {
                streamOut.writeByte(HEADER_KEY_PRIORITY_CODE);
                streamOut.writeByte(priority());
            }
            if (redelivered) {
                streamOut.writeByte(HEADER_KEY_REDELIVERED_CODE);
                streamOut.writeByte(TYPE_BOOLEAN_TRUE);
            }

            /* Output the user defined headers, i.e., the non common headers. */
            for (Map.Entry<String, Object> kv : outputHeaders.entrySet()) {
                if (kv.getKey().length() > 255) {
                    throw new MessageException("Can't write out the message as there header name length is too long");
                }

                int codeFromHeader = getCodeFromHeader(kv.getKey());
                // If the headers is under 0, it is a header with a code. */
                if (codeFromHeader > 0) {
                    streamOut.writeByte(kv.getKey().length());
                    streamOut.write(kv.getKey().getBytes(StandardCharsets.UTF_8));
                } else {
                    streamOut.writeByte(codeFromHeader);
                }
                /* Write out headers by type, encode the type, size if needed and value. */
                switch (kv.getValue().getClass().getSimpleName()) {
                    case "String":
                        final String string = (String) kv.getValue();
                        if (string.length() < 512) {
                            streamOut.writeByte(TYPE_SHORT_STRING);
                            streamOut.writeByte(string.length());
                        } else {
                            streamOut.writeByte(TYPE_STRING);
                            streamOut.writeChar(string.length());
                        }
                        streamOut.write(string.getBytes(StandardCharsets.UTF_8));
                        break;
                    case "Boolean":
                        boolean b = (boolean) kv.getValue();
                        if (b) {
                            streamOut.writeByte(TYPE_BOOLEAN_TRUE);
                        } else {
                            streamOut.writeByte(TYPE_BOOLEAN_FALSE);
                        }
                        break;
                    case "Short":
                        streamOut.writeByte(TYPE_SHORT);
                        streamOut.writeShort((Short) kv.getValue());
                        break;
                    case "Byte":
                        streamOut.writeByte(TYPE_BYTE);
                        streamOut.writeByte((Byte) kv.getValue());
                        break;
                    case "Integer":
                        int value = (int) kv.getValue();
                        if (value < RESERVED_START_TYPES && value > Byte.MIN_VALUE) {
                            streamOut.write(value);
                        } else {
                            streamOut.writeByte(TYPE_INT);
                            streamOut.writeInt(value);
                        }
                        break;
                    case "Long":
                        streamOut.writeByte(TYPE_LONG);
                        streamOut.writeLong((Long) kv.getValue());
                        break;

                    case "Float":
                        streamOut.writeByte(TYPE_FLOAT);
                        streamOut.writeFloat((Float) kv.getValue());
                        break;
                    case "Double":
                        streamOut.writeByte(TYPE_DOUBLE);
                        streamOut.writeDouble((Double) kv.getValue());
                        break;
                }
            }
            if (bodyBytes != null && bodyBytes.length > 0) {
                streamOut.writeInt(bodyBytes.length);
                streamOut.writeInt(Protocol.createHashCode(bodyBytes));
                streamOut.write(bodyBytes);
            } else {
                streamOut.writeInt(0);
                streamOut.writeInt(0);
            }
        } catch (Exception e) {
            throw new MessageException("Can't write out message", e);
        } finally {
            try {
                streamOut.close();
                bytesOutputStream.close();
            } catch (Exception e) {
            }
        }
        return bytesOutputStream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseMessageWithHeaders)) return false;
        BaseMessageWithHeaders that = (BaseMessageWithHeaders) o;
        return timestamp == that.timestamp &&
                expirationTime == that.expirationTime &&
                deliveryTime == that.deliveryTime &&
                mode == that.mode &&
                redelivered == that.redelivered &&
                priority == that.priority &&
                Objects.equals(type, that.type) && compareHeaders(that.headers) && Arrays.equals(bodyBytes, that.bodyBytes);
    }

    private boolean compareHeaders(Map<String, Object> thatHeaders) {
        if (headers == null && thatHeaders == null) return true;
        if (headers == null) return false;
        if (thatHeaders == null) return false;
        if (headers.size() != thatHeaders.size()) return false;

        for (String key : headers.keySet()) {
            Object value1 = thatHeaders.get(key);
            Object value2 = headers.get(key);
            if (value1 instanceof Number && value2 instanceof Number) {
                Number num1 = (Number) value1;
                Number num2 = (Number) value2;
                if (num1.doubleValue() != num2.doubleValue()) {
                    return false;
                }

            } else if (!value1.equals(value2)) {
                //ystem.out.println(key);
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timestamp, expirationTime, deliveryTime, mode, type, redelivered, priority, headers);
        result = 31 * result + Arrays.hashCode(bodyBytes);
        return result;
    }

    public byte[] getMessageBytes() {
        return getMessageAsBytes();
    }

    @Override
    public String toString() {

        String bodyStr = bodyBytes != null ? ", bodyBytes=" + Arrays.toString(bodyBytes) : "";

        return "BaseMessageWithHeaders{" +
                "timestamp=" + timestamp +
                ", expirationTime=" + expirationTime +
                ", deliveryTime=" + deliveryTime +
                ", mode=" + mode +
                ", type='" + type + '\'' +
                ", redelivered=" + redelivered +
                ", priority=" + priority +
                ", headers=" + headers + bodyStr +
                '}';
    }
}
