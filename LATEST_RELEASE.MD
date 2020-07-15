# 0.15.0-beta10 NATS JMS/MQ Bridge

#### TAG: 0.15.0-beta10

## Issues

* Now you can drop a jar file and it picks it up with no changes to start script for transforms #174
* Improve examples for transforms. #175
* Automatically register callback in MessageBuilder.builder().initFromMessage #183
* Connect to multiple servers. #187
* Generic way to configure IBM MQ vast array of setting for JMS connection. #182
* Keystore/Truststore passwords are being printed in logs in plain text #177


## Transform Library example improvements

To use transforms use the library messages (`implementation("io.nats.bridge:nats-jms-bridge-message:0.15.0-beta10")`).

Import the above jar file into your project. Then create a project that builds a jar file that uses
the [ServiceLoader](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html). Read about the ServiceLoader before continuing.


#### io.nats.bridge.transform.examples.ExampleTransform

```java

package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class ExampleTransform implements TransformMessage {
    @Override
    public TransformResult transform(final Message inputMessage) {
        final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
        builder.withHeader("New_Header", "New_Value");
        return TransformResult.success("Added a new header", builder.build());
    }

    @Override
    public String name() {
        return "example";
    }
}

```

As part of the Service Loader pattern you will need a resource file that is the same fully qualified class names
as the interface for the transformation `TransformMessage` as follows:

#### resources/META-INF/services/io.nats.bridge.messages.transform.TransformMessage

```
io.nats.bridge.transform.examples.ExampleTransform
```

Then you drop the jar file into the lib directory of the expanded distribution zip file or executable jar file.

Then configure the admin as follows to use this example.

#### nats-config.YAML

```yaml
...
bridges:
  - name: "natsToIBMMq"
    bridgeType: "REQUEST_REPLY"
    transforms: ["example"]
    source:
      name: "nats"
      busType: "NATS"
      subject: "b-subject"
      clusterName: "natsCluster"
    destination:
      name: "ibmMQ"
      busType: "JMS"
      subject: "DEV.QUEUE.1"
      clusterName: "ibmMqCluster"
    copyHeaders: false
    workers: 5
    tasks : 2

```

Notice `transforms: ["example"]` in the above yaml file. Dropping the new jar in the libs folder will enable to use this transform.


A more involved example showing skip, transform and no transform as well as error handling.

#### FilterByHeaderTransform.java

```java

package io.nats.bridge.transform.examples;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

class FilterByHeaderTransform implements TransformMessage {
    @Override
    public TransformResult transform(final Message inputMessage) {

        System.out.println("Class FilterByHeaderTransform: " + inputMessage.headers());
        final String headerValue = inputMessage.headers().get("H1").toString();
        System.out.println(headerValue);

        //Example of a filter message based on a header.
        if (headerValue.equals("Hello")) {
            return TransformResult.skip("H1 was hello so do not send this message");
            //Example of conditionally modifying a message.
        } else if (headerValue.equals("Goodbye")) {
            final MessageBuilder builder = MessageBuilder.builder().initFromMessage(inputMessage);
            final String newBody = inputMessage.bodyAsString() + " Goodbye";
            builder.withBody(newBody);
            return TransformResult.success("Changed body bc header was Goodbye", builder.build());

            //Example of conditionally not transforming message.
        } else if (headerValue.equals("Don't Touch")) {
            return TransformResult.notTransformed("Did not transform the message bc it said don't touch");
        } else{
            //Example did not really change.
            return TransformResult.notTransformed("Added a new header");
        }
    }

    @Override
    public String name() {
        return "all";
    }
}


```

## Generic way to set properties into IBM MQ cluster config.

The issue is we have more types and settings than expected and our ability to administer and set up IBM MQ environments is limited. So rather than be the long pole on testing and integration, provide a generic way to set connection properties.

```
io.nats.ibm.mq.jms.prop.NAME_OF_PROP
```
Where name of the prop is the exact name we pass the setPropertyX command to the connection factory.

If the prop is a float type, we call `setFloatProp`. If it is an int type, we call `setIntProp`. If it is a boolean we call `setBooleanProp`. If it is not a float, int, or boolean then we assume a string. If the type can't be determined easily, i.e., 1L vs 1 or 1.2d vs 1.2f then the end-user who does the config must specify the type in the property declaration.


```
io.nats.ibm.mq.jms.prop.int.NAME_OF_INT_PROP
io.nats.ibm.mq.jms.prop.bool.NAME_OF_BOOL_PROP
io.nats.ibm.mq.jms.prop.char.NAME_OF_CHAR_PROP
io.nats.ibm.mq.jms.prop.double.NAME_OF_DOUBLE_PROP
io.nats.ibm.mq.jms.prop.short.NAME_OF_SHORT_PROP
io.nats.ibm.mq.jms.prop.long.NAME_OF_LONG_PROP
io.nats.ibm.mq.jms.prop.byte.NAME_OF_BYTE_PROP
```


#### Example JMS connectionFactory set up
```
connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, port);
            connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
            connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
```
#### Other property types on the connection factory
```
    void setCharProperty(String var1, char var2) throws JMSException;
    void setBooleanProperty(String var1, boolean var2) throws JMSException;
    void setByteProperty(String var1, byte var2) throws JMSException;
    void setShortProperty(String var1, short var2) throws JMSException;
    void setIntProperty(String var1, int var2) throws JMSException;
    void setLongProperty(String var1, long var2) throws JMSException;
    void setFloatProp
```

#### PropertyUtils.java

```java

package io.nats.bridge.integration.ibmmq;

import com.ibm.msg.client.jms.JmsPropertyContext;

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

    public static void initJMSContext(JmsPropertyContext jmsPropertyContext, List<PropertyValue> propertyValues) {

        try {

            for (PropertyValue propertyValue : propertyValues) {

                switch (propertyValue.getPropertyType()) {
                    case INT:
                        jmsPropertyContext.setIntProperty(propertyValue.getName(), (int) propertyValue.getValue());
                        break;
                    case LONG:
                        jmsPropertyContext.setLongProperty(propertyValue.getName(), (long) propertyValue.getValue());
                        break;
                    case FLOAT:
                        jmsPropertyContext.setFloatProperty(propertyValue.getName(), (float) propertyValue.getValue());
                        break;
                    case BOOLEAN:
                        jmsPropertyContext.setBooleanProperty(propertyValue.getName(), (boolean) propertyValue.getValue());
                        break;
                    case DOUBLE:
                        jmsPropertyContext.setDoubleProperty(propertyValue.getName(), (double) propertyValue.getValue());
                        break;
                    case STRING:
                        jmsPropertyContext.setStringProperty(propertyValue.getName(), (String) propertyValue.getValue());
                        break;
                    case SHORT:
                        jmsPropertyContext.setShortProperty(propertyValue.getName(), (short) propertyValue.getValue());
                        break;
                    default:
                        jmsPropertyContext.setStringProperty(propertyValue.getName(), propertyValue.toString());
                        break;
                }
            }

        }catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

    }
}

```


It seems like MQConnectionFactory class is probably the implementation but we don't currently deal with it directly.


It looks like if we just pass ***XMSC_WMQ_CONNECTION_NAME_LIST*** and then we can use any param from the vast list of possibilities.


This approach seems like it would require the least amount of back and forth for config: https://github.com/nats-io/nats-jms-mq-bridge/issues/182.

Take a look at the MQConnectionFactory.

#### MQConnectionFactory.java

```java
package com.ibm.mq.jms;

...


public class MQConnectionFactory extends JmsJndiConnectionFactoryImpl {
    private static final long serialVersionUID = 1357803352856448349L;
    static final String sccsid2 = "@(#) MQMBID sn=p915-L200316 su=_SqjM8W1MEeqQsMc-BnOK0Q pn=com.ibm.mq.jms/src/com/ibm/mq/jms/MQConnectionFactory.java";
    private static final ObjectStreamField[] serialPersistentFields;
    protected boolean mselSet;
    protected boolean bverSet;
    protected boolean portSet;
    private transient boolean versionChangeAllowed = false;
    protected transient String connectionType = "com.ibm.msg.client.wmq";
    private static final String nullString = "IamANullString";
    protected JmsCapabilityContext capabilities = null;

    public MQConnectionFactory() {
        if (Trace.isOn) {
            Trace.entry(this, "com.ibm.mq.jms.MQConnectionFactory", "<init>()");
        }

        this.initialiseMQConnectionFactory();

        try {
            this.setIntProperty("XMSC_WMQ_CONNECTION_MODE", 0);
            this.setIntProperty("XMSC_WMQ_CONNECT_OPTIONS", 0);



...

public String getConnectionNameList() throws JMSException {
    String traceRet1 = super.getStringProperty("XMSC_WMQ_CONNECTION_NAME_LIST");
    if (Trace.isOn) {
        Trace.data(this, "com.ibm.mq.jms.MQConnectionFactory", "getConnectionNameList()", "getter", traceRet1);
    }

    return traceRet1;
}
public void setConnectionNameList(String hosts) throws JMSException {
    if (Trace.isOn) {
        Trace.data(this, "com.ibm.mq.jms.MQConnectionFactory", "setConnectionNameList(String)", "setter", hosts);
    }

    this.setConnectionNameListInternal(hosts);
    this.portSet = true;
}

private void setConnectionNameListInternal(String hosts) throws JMSException {
    if (Trace.isOn) {
        Trace.data(this, "com.ibm.mq.jms.MQConnectionFactory", "setConnectionNameListInternal(String)", "setter", hosts);
    }

    super.setStringProperty("XMSC_WMQ_CONNECTION_NAME_LIST", hosts);
    String[] listElements = this.getStringProperty("XMSC_WMQ_CONNECTION_NAME_LIST").split(",");
    String[] firstElementParts = listElements[0].split("[()]");
    if (super.getStringProperty("XMSC_WMQ_HOST_NAME") == null) {
        this.setHostName(firstElementParts[0]);
        this.setPort(Integer.parseInt(firstElementParts[1]));
    }

}
```
