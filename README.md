# nats-jms-mq-bridge


Nats MQ JMS Bridge.



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

## The bridge can encode headers 

## Wire protocol for the message body with headers

### VERSION AND MARKER - byte 0 to byte 4

* `<AB marker [byte]>`                  0
* `<CD marker [byte]>`                  1
* `<MAJOR_VERSION [byte]>`              2
* `<MINOR_VERSION[byte]>`               4


###  HEADERS          - byte 5 to N

* `<HEADER_LENGTH [ubyte]>`             5

### FIELD ENCODING  - 2 bytes

* `<HEADER_NAME_LENGTH [ubyte]>` 
* `<HEADER_TYPE [byte]>` 


### Header type short string    TYPE_SHORT_STRING(-110)
* `<STRING_LENGTH [ubyte]>` 
* `<STRING_BYTES [byte[N]]>` 

### Header type string         TYPE_STRING(-111)
* `<STRING_LENGTH [ubyte]>` 
* `<STRING_BYTES [byte[N]]>` 

### Header type boolean true   TYPE_BOOLEAN_TRUE(-112)
* `<TRUE [byte[N]]>` 

### Header type boolean false   TYPE_BOOLEAN_FALSE(-113)
* `<FALSE [ubyte]>` 

### Header type byte            TYPE_BYTE(114) 1 byte
* `<BYTE [byte]>` 

### Header type short int       TYPE_SHORT(-116) - two bytes
* `<SHORT [short]>` 

### Header type int              TYPE_INT(-118) - four bytes
* `<INT [INT]>` 

### Header type long             TYPE_LONG(-120) - eight bytes
* `<LONG [LONG]>` 

### Header type float             TYPE_FLOAT(-122) - four bytes
* `<FLOAT [FLOAT]>` 


### Header type double             TYPE_DOUBLE(-123) - eight bytes
* `<DOUBLE [DOUBLE]>` 

### No cost INT and No Cost boolean
Any type with the value lower than higher than -109 is translated
to an int type matching the same value as the type encoding. 

Boolean is encoded as either true or false as part of the type. 
The type `-112` is `true`. The type `-113` is false. 

Small int values and boolean are the most efficient. 

###  OPAQUE BODY
* `<BODY_LEN [int]>`
* `<BODY_HASH [int]>`
* `<OPAQUE_BODY_BYTES>[bytes]`