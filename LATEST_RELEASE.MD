# 0.12.0-beta8 NATS JMS/MQ Bridge

#### TAG: 0.12.0-beta8

## Issues

* #164 initial code in base lib to run transforms
* #166 Add transform to REST
* #167 initial examples
* #168 add config of transforms to builders in core


## Transform Library

To use transforms use the library messages (`implementation("io.nats.bridge:nats-jms-bridge-message:0.12.0-beta8")`).

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
