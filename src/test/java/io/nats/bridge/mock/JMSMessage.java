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

    private  Object body;

    public JMSMessage(Object body) {
        this.body = body;
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
        return (long) map.get("Timestamp");
    }

    @Override
    public void setJMSTimestamp(long timestamp) throws JMSException {
        map.put("Timestamp", timestamp);
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
    public void setJMSCorrelationID(String correlationID) throws JMSException {
        map.put("CorrelationID", correlationID);
    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        return (String) map.get("CorrelationID");
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        return (Destination) map.get("ReplyTo");
    }

    @Override
    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        map.put("ReplyTo", replyTo);
    }

    @Override
    public Destination getJMSDestination() throws JMSException {
        return (Destination) map.get("Destination");
    }

    @Override
    public void setJMSDestination(Destination destination) throws JMSException {
        map.put("Destination", destination);
    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        return (int) map.get("DeliveryMode");
    }

    @Override
    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        map.put("DeliveryMode", deliveryMode);
    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        return (boolean) map.get("Redelivered");
    }

    @Override
    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        map.put("Redelivered", redelivered);
    }

    @Override
    public String getJMSType() throws JMSException {
        return (String) map.get("Type");
    }

    @Override
    public void setJMSType(String type) throws JMSException {
        map.put("Type", type);
    }

    @Override
    public long getJMSExpiration() throws JMSException {
        return (Long) map.get("Expiration");
    }

    @Override
    public void setJMSExpiration(long expiration) throws JMSException {
        map.put("Expiration", expiration);
    }

    @Override
    public long getJMSDeliveryTime() throws JMSException {
        return (long)map.get("DeliveryTime");
    }

    @Override
    public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
        map.put("DeliveryTime", deliveryTime);
    }

    @Override
    public int getJMSPriority() throws JMSException {
        return (int) map.get("Priority");
    }

    @Override
    public void setJMSPriority(int priority) throws JMSException {
        map.put("Priority", priority);
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
