# nats-jms-mq-bridge


Nats MQ JMS Bridge.



## Early version 

The focus is on forwarding `request/reply` message from `JMS and IBM MQ` to `nats.io`.

1. A request gets sent to `nats.io` which then sends that request to IBM/MQ/JMS. 
2. Bridge gets the response and sends it back to the original client. 
3. The focus is on Queues and Request/Reply. 


#### Basic flow 

```ascii

                        +-------------------+         +-----------------+ 3      +-----------+ 4      +------------+
                        |                   | 2       |                 |        |           |sendTo  |            |
                        | Nats Server       |sendToQueue                |send    |JMS Server |Queue   |   ServiceA |
                        |  subject serivceA +---------> NatsMqJMSBridge +------->+           +-------->            |
                        |                   |         |                 |        |Queue      |        |            |
+----------+ request 1  |                   |publish  |                 |        |ServiceA   |        |            |
|          +------------>                   +<--------+                 <--------+           <--------+            |
| Client   |            |                   | 8       |                 | sendTo |           | send   |            |
|          |            |                   |         |                 | Response           |  5     |            |
|          <------------+                   |         |                 | Queue  |           |        |            |
+----------+ sendTo     +-------------------+         +-----------------+  6     +-----------+        +------------+
             Response
             Queue
               9



```

This all happens async.

## Dev Note

To deploy to local repo to integrate with admin use:

```sh
./gradlew clean build publishToMavenLocal
./gradlew clean build publishToMavenLocal -x test
```

## The bridge can encode headers

## Wire protocol for the message body with headers

### VERSION AND MARKER - byte 0 to byte 4

* `<AB marker [byte]>`                  0
* `<CD marker [byte]>`                  1
* `<MAJOR_VERSION [byte]>`              2
* `<MINOR_VERSION[byte]>`               4


###  HEADERS          - byte 5 to N

* `<HEADER_LENGTH [ubyte]>`             5

### FIELD ENCODING  - 2 bytes

* `<HEADER_NAME_LENGTH [ubyte]>` 
* `<HEADER_TYPE [byte]>` 


### Header type short string    TYPE_SHORT_STRING(-110)
* `<STRING_LENGTH [ubyte]>` 
* `<STRING_BYTES [byte[N]]>` 

### Header type string         TYPE_STRING(-111)
* `<STRING_LENGTH [ubyte]>` 
* `<STRING_BYTES [byte[N]]>` 

### Header type boolean true   TYPE_BOOLEAN_TRUE(-112)
* `<TRUE [byte[N]]>` 

### Header type boolean false   TYPE_BOOLEAN_FALSE(-113)
* `<FALSE [ubyte]>` 

### Header type byte            TYPE_BYTE(114) 1 byte
* `<BYTE [byte]>` 

### Header type short int       TYPE_SHORT(-116) - two bytes
* `<SHORT [short]>` 

### Header type int              TYPE_INT(-118) - four bytes
* `<INT [INT]>` 

### Header type long             TYPE_LONG(-120) - eight bytes
* `<LONG [LONG]>` 

### Header type float             TYPE_FLOAT(-122) - four bytes
* `<FLOAT [FLOAT]>` 


### Header type double             TYPE_DOUBLE(-123) - eight bytes
* `<DOUBLE [DOUBLE]>` 

### No cost INT and No Cost boolean
Any type with the value lower than higher than -109 is translated
to an int type matching the same value as the type encoding. 

Boolean is encoded as either true or false as part of the type. 
The type `-112` is `true`. The type `-113` is false. 

Small int values and boolean are the most efficient. 

###  OPAQUE BODY
* `<BODY_LEN [int]>`
* `<BODY_HASH [int]>`
* `<OPAQUE_BODY_BYTES>[bytes]`

## For common headers the protocol just writes short header codes 

#### Header Codes 

```java
public class Protocol {

    public static final int HEADER_KEY_DELIVERY_TIME_CODE = -128;
    public static final int HEADER_KEY_TIMESTAMP_CODE = -127;
    public static final int HEADER_KEY_MODE_CODE = -126;
    public static final int HEADER_KEY_EXPIRATION_TIME_CODE = -125;
    public static final int HEADER_KEY_TYPE_CODE = -124;
    public static final int HEADER_KEY_PRIORITY_CODE = -123;
    public static final int HEADER_KEY_REDELIVERED_CODE = -122;
    static {
        commonHeaders.put(HEADER_KEY_DELIVERY_TIME, HEADER_KEY_DELIVERY_TIME_CODE);
        commonHeaders.put(HEADER_KEY_TIMESTAMP, HEADER_KEY_TIMESTAMP_CODE);
        commonHeaders.put(HEADER_KEY_MODE, HEADER_KEY_MODE_CODE);
        commonHeaders.put(HEADER_KEY_EXPIRATION_TIME, HEADER_KEY_EXPIRATION_TIME_CODE);
        commonHeaders.put(HEADER_KEY_TYPE, HEADER_KEY_TYPE_CODE);
        commonHeaders.put(HEADER_KEY_PRIORITY, HEADER_KEY_PRIORITY_CODE);
        commonHeaders.put(HEADER_KEY_REDELIVERED, HEADER_KEY_REDELIVERED_CODE);
        commonHeaders.put("ENV", -121);
        commonHeaders.put("H1", -120);
        commonHeaders.put("H2", -119);
        commonHeaders.put("H3", -117);
        commonHeaders.put("H4", -116);
        commonHeaders.put("ENVIRONMENT", -115);
    }
}

``` 

User can use "ENV", "H1" (header1) through "H4", and "ENVIRONMENT" as common headers that will be encoded in one byte. 

#### Protocol markers  

```java

public class Protocol {

    public static final int MESSAGE_VERSION_MAJOR = 1;
    public static final int MESSAGE_VERSION_MINOR = 0;
    public static final int MARKER_AB = (byte) 0xab;
    public static final int MARKER_CD = (byte) 0xcd;
}
```

#### Protocol type codes   

```java

public class Protocol {

    public static final int RESERVED_START_TYPES = -109;
    public static final int TYPE_SHORT_STRING = -110;
    public static final int TYPE_STRING = -111;
    public static final int TYPE_BOOLEAN_TRUE = -112;
    public static final int TYPE_BOOLEAN_FALSE = -113;
    public static final int TYPE_BYTE = -114;
    //public static final int TYPE_UNSIGNED_BYTE = -115; RESERVED
    public static final int TYPE_SHORT = -116;
    //public static final int TYPE_UNSIGNED_SHORT = -117;
    public static final int TYPE_INT = -118;
    //public static final int TYPE_UNSIGNED_INT = -119; RESERVED
    public static final int TYPE_LONG = -120;
    //public static final int TYPE_UNSIGNED_LONG = -121; RESERVED
    public static final int TYPE_FLOAT = -122;
    public static final int TYPE_DOUBLE = -123;
    //public static final int RESERVED_END_TYPES = -128;
}

```

Reserved type codes are between `-109` to `-128`. 
Unsigned `byte`, `short`, `int` and `long` are reserved but not implemented. 


#### This code converts a Bridge Message into a byte array. 

```java
public class BaseMessageWithHeaders implements BytesMessage {

    /**
     * Convert byte representation of Message
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
            if (priority != -1)headerSize++;
            if (redelivered) headerSize++;

            headerSize += outputHeaders.size();
            streamOut.writeByte(headerSize);

            /* Limit headers we are sending to the size of 1 unsigned byte. */
            if (headerSize > 255) throw new MessageException("Can't write out the message as there are too many headers");

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
                if (codeFromHeader>0) {
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
                        if (value < RESERVED_START_TYPES &&  value > Byte.MIN_VALUE) {
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
}
```

#### This code converts a byte array into a message 

```java

public class MessageBuilder {

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
}
```

