package io.nats.bridge.integration.ibmmq;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyUtils {

    public static final String PROP_PREFIX = "io.nats.ibm.mq.jms.prop.";
    public static List<PropertyValue> extractProperties(final Hashtable<String, String> jndiProperties)  {

        final List<String> dynamicProperties = jndiProperties.keySet().stream()
                .filter(key -> key.startsWith(PROP_PREFIX))
                .map(
                        key -> key.substring(PROP_PREFIX.length())
                )
                .collect(Collectors.toList());


        System.out.println(dynamicProperties);

        List<PropertyValue> propValues = new ArrayList<>(dynamicProperties.size());

        for (String prop : dynamicProperties) {
            final String key = PROP_PREFIX + prop;
            PropertyType type = null;
            Object finalValue = null;
            String propName = prop;
            final String value = jndiProperties.get(key);

            if (prop.contains(".")) {

                String[] split = prop.split("\\.");

                if (split.length != 2) {
                    throw new IllegalStateException("Badly formed property key " + key);
                }

                String typeName = split[0];
                propName = split[1];


                switch (typeName) {
                    case "int" :
                        type = PropertyType.INT;
                        finalValue = Integer.parseInt(value);
                        break;
                    case "float" :
                        type = PropertyType.FLOAT;
                        finalValue = Float.parseFloat(value);
                        break;
                    case "long" :
                        type = PropertyType.LONG;
                        finalValue = Long.parseLong(value);
                        break;
                    case "boolean" :
                        type = PropertyType.BOOLEAN;
                        finalValue = Boolean.parseBoolean(value);
                        break;
                    case "double" :
                        type = PropertyType.DOUBLE;
                        finalValue = Double.parseDouble(value);
                        break;
                    case "string":
                        type = PropertyType.STRING;
                        finalValue = value;
                        break;
                    case "short" :
                        type = PropertyType.SHORT;
                        finalValue = Short.parseShort(value);
                        break;
                    default:
                        throw new IllegalStateException("Invalid Type String " + typeName);

                }


            } else {

                if (value.isEmpty()) {
                    type = PropertyType.STRING;
                    finalValue = value;
                } else {
                    char c = value.charAt(0);
                    if (Character.isDigit(c) && value.contains(".")) {
                        try {
                            finalValue = Float.parseFloat(value);
                            type = PropertyType.FLOAT;
                        } catch (Exception ex) {
                            type = PropertyType.STRING;
                            finalValue = value;
                        }

                    } else if (Character.isDigit(c)) {
                        try {
                            finalValue = Integer.parseInt(value);
                            type = PropertyType.INT;
                        } catch (Exception ex) {
                            type = PropertyType.STRING;
                            finalValue = value;
                        }
                    } else if (c == 't' || c == 'T' || c == 'F' || c == 'f') {
                        try {
                            finalValue = Boolean.parseBoolean(value);
                            type = PropertyType.BOOLEAN;
                        } catch (Exception ex) {
                            type = PropertyType.STRING;
                            finalValue = value;
                        }
                    } else {
                        type = PropertyType.STRING;
                        finalValue = value;
                    }
                }
            }

            propValues.add(new PropertyValue(propName, type, finalValue));
        }

        return propValues;

    }
}
