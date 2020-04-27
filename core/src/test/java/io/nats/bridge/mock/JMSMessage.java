package io.nats.bridge.mock;

import javax.jms.Destination;
import javax.jms.JMSException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JMSMessage implements javax.jms.Message {


    private final Map<String, Object> map = new HashMap<>();

    private Object body;

    private long timestamp;
    //TTL plus timestamp
    private long expirationTime;
    //Delivery time is not instant
    private long deliveryTime;
    private int mode;
    private String type;
    private boolean redelivered;
    private int priority;
    private Destination destination;
    private Destination replyDest;
    private String correlationID;


    public JMSMessage(Object body) {
        this.body = body;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public Object getBody() {
        return body;
    }

    public JMSMessage setBody(Object body) {
        this.body = body;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JMSMessage withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public JMSMessage withExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    public long getDeliveryTime() {
        return deliveryTime;
    }

    public JMSMessage withDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
        return this;
    }

    public int getMode() {
        return mode;
    }

    public JMSMessage withMode(int mode) {
        this.mode = mode;
        return this;
    }

    public String getType() {
        return type;
    }

    public JMSMessage withType(String type) {
        this.type = type;
        return this;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public JMSMessage withRedelivered(boolean redelivered) {
        this.redelivered = redelivered;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public JMSMessage withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public String getJMSMessageID() throws JMSException {
        return (String) map.get("MessageID");
    }

    @Override
    public void setJMSMessageID(String id) throws JMSException {
        map.put("MessageID", id);
    }

    @Override
    public long getJMSTimestamp() throws JMSException {
        return timestamp;
    }

    @Override
    public void setJMSTimestamp(long timestamp) throws JMSException {
        this.timestamp = timestamp;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return getJMSCorrelationID().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        this.setJMSCorrelationID(new String(correlationID, StandardCharsets.UTF_8));
    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        return correlationID;
    }

    @Override
    public void setJMSCorrelationID(String correlationID) throws JMSException {
        this.correlationID = correlationID;
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        return replyDest;
    }

    @Override
    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        replyDest = replyTo;
    }

    @Override
    public Destination getJMSDestination() throws JMSException {
        return destination;
    }

    @Override
    public void setJMSDestination(final Destination destination) throws JMSException {
        this.destination = destination;
    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        return mode;
    }

    @Override
    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        this.mode = deliveryMode;
    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        return redelivered;
    }

    @Override
    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        this.redelivered = redelivered;
    }

    @Override
    public String getJMSType() throws JMSException {
        return type;
    }

    @Override
    public void setJMSType(String type) throws JMSException {
        this.type = type;
    }

    @Override
    public long getJMSExpiration() throws JMSException {
        return expirationTime;
    }

    @Override
    public void setJMSExpiration(long expiration) throws JMSException {
        expirationTime = expiration;
    }

    @Override
    public long getJMSDeliveryTime() throws JMSException {
        return deliveryTime;
    }

    @Override
    public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
        this.deliveryTime = deliveryTime;
    }

    @Override
    public int getJMSPriority() throws JMSException {
        return priority;
    }

    @Override
    public void setJMSPriority(int priority) throws JMSException {
        this.priority = priority;
    }

    @Override
    public void clearProperties() throws JMSException {
        map.clear();
    }

    @Override
    public boolean propertyExists(String name) throws JMSException {
        return map.containsKey(name);
    }

    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        return (boolean) map.get(name);
    }

    @Override
    public byte getByteProperty(String name) throws JMSException {
        return (byte) map.get(name);
    }

    @Override
    public short getShortProperty(String name) throws JMSException {
        return (short) map.get(name);
    }

    @Override
    public int getIntProperty(String name) throws JMSException {
        return (int) map.get(name);
    }

    @Override
    public long getLongProperty(String name) throws JMSException {
        return (long) map.get(name);
    }

    @Override
    public float getFloatProperty(String name) throws JMSException {
        return (float) map.get(name);
    }

    @Override
    public double getDoubleProperty(String name) throws JMSException {
        return (double) map.get(name);
    }

    @Override
    public String getStringProperty(String name) throws JMSException {
        return (String) map.get(name);
    }

    @Override
    public Object getObjectProperty(String name) throws JMSException {
        return map.get(name);
    }

    @Override
    public Enumeration getPropertyNames() throws JMSException {

        final Iterator<String> iterator = map.keySet().iterator();

        return new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public Object nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public void setBooleanProperty(String name, boolean value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setByteProperty(String name, byte value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setShortProperty(String name, short value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setIntProperty(String name, int value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setLongProperty(String name, long value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setFloatProperty(String name, float value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setDoubleProperty(String name, double value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setStringProperty(String name, String value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void setObjectProperty(String name, Object value) throws JMSException {
        map.put(name, value);
    }

    @Override
    public void acknowledge() throws JMSException {

    }

    @Override
    public void clearBody() throws JMSException {

        body = null;
    }


    @Override
    public <T> T getBody(Class<T> c) throws JMSException {
        return (T) body;
    }

    @Override
    public boolean isBodyAssignableTo(Class c) throws JMSException {
        return body.getClass().isAssignableFrom(c);
    }
}
