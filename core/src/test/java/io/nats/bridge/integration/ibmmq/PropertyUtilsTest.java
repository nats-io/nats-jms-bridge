package io.nats.bridge.integration.ibmmq;


import org.junit.Test;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class PropertyUtilsTest {

    @Test
    public void testSimple() {

        final Hashtable<String, String> ht = new Hashtable<String, String>();

        ht.put(PropertyUtils.PROP_PREFIX+"one", "1");
        ht.put(PropertyUtils.PROP_PREFIX+"int.three", "3");
        ht.put(PropertyUtils.PROP_PREFIX+"boolean.four", "true");
        ht.put(PropertyUtils.PROP_PREFIX+"five", "1.1");
        ht.put(PropertyUtils.PROP_PREFIX+"long.six", "1");
        ht.put(PropertyUtils.PROP_PREFIX+"double.seven", "7.0");
        ht.put(PropertyUtils.PROP_PREFIX+"float.eight", "8.0");
        ht.put(PropertyUtils.PROP_PREFIX+"short.nine", "9");
        ht.put(PropertyUtils.PROP_PREFIX+"string.ten", "ten");
        ht.put(PropertyUtils.PROP_PREFIX+"boolean.eleven", "true");
        ht.put(PropertyUtils.PROP_PREFIX+"twelve", "true");

        List<PropertyValue> propertyValues = PropertyUtils.extractProperties(ht);

        Map<String, PropertyValue> map = propertyValues.stream().collect(Collectors.toMap(PropertyValue::getName, pv -> pv));

        assertEquals(true, map.get("four").getValue());
        assertEquals("four", map.get("four").getName());
        assertEquals(PropertyType.BOOLEAN, map.get("four").getPropertyType());


        assertEquals(1, map.get("one").getValue());
        assertEquals("one", map.get("one").getName());
        assertEquals(PropertyType.INT, map.get("one").getPropertyType());

        assertEquals(3, map.get("three").getValue());
        assertEquals("three", map.get("three").getName());
        assertEquals(PropertyType.INT, map.get("three").getPropertyType());

    }

}