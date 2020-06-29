package io.nats.bridge.integration.ibmmq;

public class PropertyValue {

    private final String name;
    private final PropertyType propertyType;
    private final Object value;

    public PropertyValue(String name, PropertyType propertyType, Object value) {
        this.name = name;
        this.propertyType = propertyType;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "PropertyValue{" +
                "name='" + name + '\'' +
                ", propertyType=" + propertyType +
                ", value=" + value +
                '}';
    }
}
