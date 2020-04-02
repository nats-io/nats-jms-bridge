package io.nats.bridge.messages;

import java.util.Map;

public class BaseMessageWithHeaders implements BytesMessage {

    private final long timestamp;
    //TTL plus timestamp
    private final  long expirationTime;
    //Delivery time is not instant
    private final  long deliveryTime;
    private final  int mode;
    private final  String type;
    private final  boolean redelivered;
    private final  int priority;
    private final  Map<String, Object> headers;

    private final byte [] bytes;


    public BaseMessageWithHeaders(long timestamp, long expirationTime, long deliveryTime, int mode, String type,
                                  boolean redelivered, int priority, Map<String, Object> headers, byte[] bytes) {
        this.timestamp = timestamp;
        this.expirationTime = expirationTime;
        this.deliveryTime = deliveryTime;
        this.mode = mode;
        this.type = type;
        this.redelivered = redelivered;
        this.priority = priority;
        this.headers = headers;
        this.bytes = bytes;
    }

    public  byte [] getBodyBytes() {
        return bytes;
    }


    public long timestamp() {return timestamp;}
    //TTL plus timestamp
    public long expirationTime() {return expirationTime;}
    //Delivery time is not instant
    public long deliveryTime() {return deliveryTime;}
    public int mode() {return mode;}
    public String type() {return type;}
    public boolean redelivered() {return redelivered;}
    public int priority() {return priority;}
    public Map<String, Object> headers() {return headers;}


}
