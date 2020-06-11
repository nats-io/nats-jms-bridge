package io.nats.bridge.messages;

import java.util.HashMap;
import java.util.Map;

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

    public static final int HEADER_KEY_DELIVERY_TIME_CODE = -128;
    public static final int HEADER_KEY_TIMESTAMP_CODE = -127;
    public static final int HEADER_KEY_MODE_CODE = -126;
    public static final int HEADER_KEY_EXPIRATION_TIME_CODE = -125;
    public static final int HEADER_KEY_TYPE_CODE = -124;
    public static final int HEADER_KEY_PRIORITY_CODE = -123;
    public static final int HEADER_KEY_REDELIVERED_CODE = -122;
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
    //Map for now, I will change to array to avoid boxing.
    private static Map<String, Integer> commonHeaders = new HashMap<>();
    public static final Map<String, Integer> COMMON_HEADERS = commonHeaders;
    //Map for now, I will change to array to avoid boxing.
    private static Map<Integer, String> commonCodesToHeaders = new HashMap<>();
    public static final Map<Integer, String> COMMON_HEADER_CODES = commonCodesToHeaders;

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

    static {
        commonCodesToHeaders.put(HEADER_KEY_DELIVERY_TIME_CODE, HEADER_KEY_DELIVERY_TIME);
        commonCodesToHeaders.put(HEADER_KEY_TIMESTAMP_CODE, HEADER_KEY_TIMESTAMP);
        commonCodesToHeaders.put(HEADER_KEY_MODE_CODE, HEADER_KEY_MODE);
        commonCodesToHeaders.put(HEADER_KEY_EXPIRATION_TIME_CODE, HEADER_KEY_EXPIRATION_TIME);
        commonCodesToHeaders.put(HEADER_KEY_TYPE_CODE, HEADER_KEY_TYPE);
        commonCodesToHeaders.put(HEADER_KEY_PRIORITY_CODE, HEADER_KEY_PRIORITY);
        commonCodesToHeaders.put(HEADER_KEY_REDELIVERED_CODE, HEADER_KEY_REDELIVERED);
        commonCodesToHeaders.put(-121, "ENV");
        commonCodesToHeaders.put(-120, "H1");
        commonCodesToHeaders.put(-119, "H2");
        commonCodesToHeaders.put(-117, "H3");
        commonCodesToHeaders.put(-116, "H4");
        commonCodesToHeaders.put(-115, "ENVIRONMENT");
    }

    static int getCodeFromHeader(String header) {
        return COMMON_HEADERS.getOrDefault(header, header.length());
    }

    static String getHeaderFromCode(int headerCode) {
        return COMMON_HEADER_CODES.get(headerCode);
    }
    //public static final int RESERVED_END_TYPES = -128;

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
