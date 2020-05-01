# nats-jms-mq-bridge


Nats MQ JMS Bridge.


## Travis Build

![Build Status](https://api.travis-ci.org/nats-io/nats-jms-mq-bridge.svg "Build Status")



## Early version

The focus is on forwarding `request/reply` message from `JMS and IBM MQ` to `nats.io`.

1. A request gets sent to `nats.io` which then sends that request to IBM/MQ/JMS.
2. Bridge gets the response and sends it back to the original client.
3. The focus is on Queues and Request/Reply.


#### Basic flow

```ascii

                        +-------------------+         +-----------------+ 3      +-----------+ 4      +------------+
                        |                   | 2       |                 |        |           |sendTo  |            |
                        | Nats Server       |sendToQueue                |send    |JMS Server |Queue   |   ServiceA |
                        |  subject serivceA +---------> NatsMqJMSBridge +------->+           +-------->            |
                        |                   |         |                 |        |Queue      |        |            |
+----------+ request 1  |                   |publish  |                 |        |ServiceA   |        |            |
|          +------------>                   +<--------+                 <--------+           <--------+            |
| Client   |            |                   | 8       |                 | sendTo |           | send   |            |
|          |            |                   |         |                 | Response           |  5     |            |
|          <------------+                   |         |                 | Queue  |           |        |            |
+----------+ sendTo     +-------------------+         +-----------------+  6     +-----------+        +------------+
             Response
             Queue
               9



```

This all happens async.

Admin Console
![image](https://user-images.githubusercontent.com/382678/80275243-e3010c80-8694-11ea-843c-b26cf43cf8ae.png)
