# nats-jms-mq-bridge


Nats MQ JMS Bridge.

## Alpha Release Notes

Nats Bridge is releasing an Alpha JMS / IBM MQ bridge on May 1st, 2020.


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
