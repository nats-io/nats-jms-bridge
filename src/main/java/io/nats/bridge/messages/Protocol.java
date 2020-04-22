package io.nats.bridge.messages;

public class Protocol {

    public static final int MESSAGE_VERSION_MAJOR = 1;
    public static final int MESSAGE_VERSION_MINOR = 0;
    public static final int MARKER_AB = (byte) 0xab;
    public static final int MARKER_CD = (byte) 0xcd;

    public static final String HEADER_KEY_DELIVERY_TIME = "BRIDGE_deliveryTime";
    public static final String HEADER_KEY_TIMESTAMP = "BRIDGE_timestamp";
    public static final String HEADER_KEY_MODE = "BRIDGE_mode";
    public static final String HEADER_KEY_EXPIRATION_TIME = "BRIDGE_expirationTime";
    public static final String HEADER_KEY_TYPE = "BRIDGE_type";
    public static final String HEADER_KEY_PRIORITY = "BRIDGE_priority";
    public static final String HEADER_KEY_REDELIVERED = "BRIDGE_redelivered";


    public static final int RESERVED_START_TYPES = -109;
    public static final int TYPE_SHORT_STRING = -110;
    public static final int TYPE_STRING = -111;
    public static final int TYPE_BOOLEAN_TRUE = -112;
    public static final int TYPE_BOOLEAN_FALSE = -113;
    public static final int TYPE_BYTE = -114;
    //public static final int TYPE_UNSIGNED_BYTE = -115;
    public static final int TYPE_SHORT = -116;
    public static final int TYPE_UNSIGNED_SHORT = -117;
    public static final int TYPE_INT = -118;
    //public static final int TYPE_UNSIGNED_INT = -119;
    public static final int TYPE_LONG = -120;
    //public static final int TYPE_UNSIGNED_LONG = -121;
    public static final int TYPE_FLOAT = -122;
    public static final int TYPE_DOUBLE = -123;
    public static final int RESERVED_END_TYPES = -128;




    public static int createHashCode(byte[] value) {
        int h = 0;
        byte[] var2 = value;
        int var3 = value.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            byte v = var2[var4];
            h = 31 * h + (v & 255);
        }

        return h;
    }

}
