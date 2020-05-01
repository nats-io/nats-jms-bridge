# NATS JMS/MQ Bridge.

NATS MQ JMS Bridge.


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

# 0.3.0-Alpha1 NATS JMS/MQ Bridge


This release is an [alpha release](https://en.wikipedia.org/wiki/Software_release_life_cycle). This [alpha release](https://stackoverflow.com/questions/40067469/what-is-the-difference-between-alpha-and-beta-release) lacks perf testing and full integration testing. There are known shortcomings. It is a work in progress. 

In the core NATS bridge, there are working integration tests to Bridge REQUEST/REPLY queues between NATS and IBM MQ as well as ActiveMQ JMS. This bridging is bi-directional. The bridge can perform a QUEUE to QUEUE FORWARD or a REQUEST/REPLAY DELEGATION. 

The NATS JMS/MQ Bridge is broken up into two parts. The `admin` and the `core` lib. The `admin` is written with Spring Boot and consists of an executable jar file. 





Artifact | Description | Type
-- | -- | --
nats-bridge-admin-0.3.0-ALPHA1.tar | Application with start up script and lib folder. Start up scripts, utilities and libs expanded. | Spring Boot App
nats-bridge-admin-0.3.0-ALPHA1.zip | Application with start up script and lib folder. Start up scripts, utilities and libs expanded. | Spring Boot App
nats-bridge-admin-boot-0.3.0-ALPHA1.tar | Application with start up script and lib folder. Single executable jar files. No libs. | Spring Boot App
nats-bridge-admin-boot-0.3.0-ALPHA1.zip | Application with start up script and lib folder. Single executable jar files. No libs. | Spring Boot App
nats-bridge-admin-0.3.0-ALPHA1-boot.jar | Executable jar file | Spring Boot App
nats-bridge-admin-0.3.0-ALPHA1.jar | Admin classes only | Lib
nats-jms-bridge-0.3.0-ALPHA1.jar | Core Bridge Classes Only | Lib
nats-jms-bridge-0.3.0-ALPHA1.pom | Maven POM file for Bridge | Manifest

# Admin Console for NATS JMS Bridge 

![image](https://user-images.githubusercontent.com/382678/80275243-e3010c80-8694-11ea-843c-b26cf43cf8ae.png)

# Admin features

The admin provides a REST interface. It is meant to be run behind NGINX or Apache (or some load balancer / reverse proxy that does SSL / TLS termination). The admin provides an admin user that can create other users. Users can generate a secure JWT token which they can use to use command tools that hit the admin's REST interface. 

The admin emits metrics which are configured to be picked up with tools like Prometheus, DataDog, InfluxDB, CloudWatch, etc. See the CICD directory to see example docker-deploys that use Prometheus.

## Install guide 


## Download the distribution zip and unzip it

```sh

mkdir bridge 
cd bridge 

wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.3.0-Alpha1/nats-bridge-admin-0.3.0-ALPHA1.zip
unzip nats-bridge-admin-0.3.0-ALPHA1.zip
rm *.zip 
```

## Before you run the server 

Before you run the server you may want to download the source code and run the `docker-compose` out of 
the `cicd` folder which starts up IBM MQ, ActiveMQ and NATS Servers in Docker. 

```sh
git clone https://github.com/nats-io/nats-jms-mq-bridge.git
cd nats-jms-mq-bridge
docker-compose -f cicd/docker-compose-local-dev.yml up
```




## Run the application

```sh
cd nats-bridge-admin-0.3.0-ALPHA1

## Copy the test servers the correspond to the docker compose earlier 
mv nats-bridge.yaml config 

## Run the server 
bin/nats-bridge-admin
```

```sh

 .   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
 '  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::        (v2.2.6.RELEASE)

2020-05-01 03:22:06.114  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Starting ApplicationMain on Richards-MacBook-Pro.local with PID 92828 (/Users/richardhightower/bridge/nats-bridge-admin-0.3.0-ALPHA1/lib/nats-bridge-admin-0.3.0-ALPHA1.jar started by richardhightower in /Users/richardhightower/bridge/nats-bridge-admin-0.3.0-ALPHA1)
2
...
2020-05-01 03:22:09.211  INFO 92828 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2020-05-01 03:22:09.214  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Started ApplicationMain in 3.409 seconds (JVM running for 3.688)
```


## Run an integration test 

```sh

bin/integration.sh

```

If all goes well, you should see this output. 

```sh
======== Counts for nats =========== 
                 publish_count          0
                 request_count         90
        request_response_count         91
                received_count          0
          received_reply_count          0
   received_reply_count_errors          0
======== Gauges for nats =========== 
======== Timers for nats =========== 
       request_response_timing         53
          receive_reply_timing          0
REPLY COUNT 100
Running? true
Started? true
Errors? false

```



## Using the command line tools 
To use this tool you must install `jq`.
`jq` is a lightweight command-line JSON processor.
https://stedolan.github.io/jq/
(brew install jq or sudo apt-get install jq or https://stedolan.github.io/jq/download/)

## To set up admin tool for the first time from the NATS Bridge Admin directory run `set-up-admin`

```sh

  $ pwd
    /opt/synadia/nats-bridge/admin

  $ bin/admin.sh set-up-admin

```

## To generate a token for a user use `generate-token`

```sh

  $ bin/admin.sh SUBJECT PUBLIC_KEY SECRET

```
## To check server health run `health`

```sh

  $ bin/admin.sh health

  {
    "status": "UP"
  }
```

## To see the server config run `config`

```
 $ bin/admin.sh config

  {
    "name": "Starter Config",
    "dateTime": "2020-04-24T20:38:50.574",
    "bridges": [
      {
        "name": "jmsToNatsSample",
        "bridgeType": "REQUEST_REPLY",
        "source": {
        ...
```

## To see if the bridge is running run `running`.

```sh
   $ bin/admin.sh running

    {
      "message": "Running?",
      "flag": true
    }

```

## To see if the bridge had any errors  run `was-error`.

```sh
  $ bin/admin.sh was-error

    {
      "message": "Errors?",
      "flag": false
    }
```

## To see the last error run `last-error`

```sh

   $ bin/admin.sh last-error
    {
      "message": "ERROR",
      "error": {
        "name": "JMSMessageBusException",
        "message": "Error receiving message",
        "root": "AMQ219017: Consumer is closed"
      }
    }

```

## To clear the last error use `clear-error`

```sh
  
$ bin/admin.sh clear-error

```
## To restart the bridge use `restart`

```sh

  $ bin/admin.sh restart
```

## To Stop the bridge use `stop` (use restart to start it again)

```sh 
  $ bin/admin.sh stop
```

## To See KPI and metrics for the admin 

```sh 
  $ bin/admin.sh metrics 

```

## To import a TSV file 

```sh
 $ bin/admin.sh import tab myimport.tsv 
``` 

## To import a CSV file 

```sh
 $ bin/admin.sh import tab myimport.csv 
``` 

The file format of the TSV and CSV is described below. 

# Nats JMS MQ Bridge Service service

This is the source code for the Nats JMS MQ Bridge Service Service back-end application developed with Spring Boot. 

You can build the project and run tests by running:

		$ ./gradlew build

You can launch the application by running:

		$ ./gradlew bootRun

All development happens in the development branch.

API documentation is in swagger and accessible via the url:  HTTP://application_base_url/swagger-ui.html#!


## Swagger 

http://localhost:8080/swagger-ui.html#!  

## Test with curl 

## Send ping 

```sh
$ curl localhost:8080/ping

## Output 
pong
```

## Curl root 

```sh
 $ curl localhost:8080/    

## Output
<html><body><H1>NATS JMS BRIDGE ADMIN</H1> <p><a href="./swagger-ui.html#!">Click Here</a></p></body></html>

```

## Show Auth needs a token

```sh
$  curl localhost:8080/api/v1/auth/ping  | jq .

```

### Output 

```json
{
  "timestamp": "2020-04-23T05:51:49.096+0000",
  "status": 403,
  "error": "Forbidden",
  "message": "No JWT token found in request headers",
  "path": "/api/v1/auth/ping"
}

```


## Generate JWT (Admin)

`        val loginToken = LoginToken("Rick Hightower", listOf(Role("Admin")))`

```sh
   curl -X POST -H "Content-Type: application/json"\
                -d '{"subject":"Rick Hightower", "publicKey" : "foobar" }'\
                localhost:8080/api/v1/login/generateToken 
```

### Output

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJSaWNrIiwicm9sZXMiOiJBZG1pbiJ9.hd2yFD_aEDL5IVaTz0TpiqxTTdZ4CteDLp1wquDTabI",
  "subject": "Rick Hightower"
}

```


## Generate JWT (User)

`        val loginToken = LoginToken("Paul Hix", listOf(Role("User")))`

```sh
   curl -X POST -H "Content-Type: application/json"\
                -d '{"subject":"Paul Hix", "publicKey" : "iloverick" }'\
                localhost:8080/api/v1/login/generateToken 
```

### Output

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJQYXVsIEhpeCIsInJvbGVzIjoiVXNlciJ9.Pb02geRU-RPyHyGwgxlwnptJ66zAs4nbdR7z53JY6RU",
  "subject": "Paul Hix"
}


```



## Actual Admin 


```sh
 $ curl -X POST -H "Content-Type: application/json"\
    -d '{"subject":"admin", "publicKey" : "pk-56de4859-0e0e-485b-acc0-c515ac17546b", "secret":"sk-0b483b4f-106e-437c-b1b5-6cf55e9fbd1e" }'\
        http://localhost:8080/api/v1/login/generateToken | jq .

```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJhZG1pbiIsInJvbGVzIjoiQWRtaW4ifQ.UniWFiHOof9NkkMvV18Ch-r7Jm6UAcg1JfjrkUIWEk8",
  "publicKey": "pk-e0f0ffb4-c257-4c61-8a9f-268b00669dbb",
  "subject": "admin"
}
```

## Get config list 

To hit the swagger UI direct when in dev use this: 
* ADMIN 
    * Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJSaWNrIiwicm9sZXMiOiJBZG1pbiJ9.hd2yFD_aEDL5IVaTz0TpiqxTTdZ4CteDLp1wquDTabI
* USER
    * Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJQYXVsIEhpeCIsInJvbGVzIjoiVXNlciJ9.Pb02geRU-RPyHyGwgxlwnptJ66zAs4nbdR7z53JY6RU
* 512
    * Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJhZG1pbiIsInJvbGVzIjoiQWRtaW4ifQ.RPRNPM3wPFlkkcBgOHemfpDCWPCM5RKU60TwA9Z04UrehhYJBMkPmMOGWWRhBj0Csk8cMxJhvsq5kj7-ffF-Hw


#### From User
```sh
   curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJQYXVsIEhpeCIsInJvbGVzIjoiVXNlciJ9.Pb02geRU-RPyHyGwgxlwnptJ66zAs4nbdR7z53JY6RU"\
          localhost:8080/api/v1/bridges/admin/config 
```



### Output 

```json
{
  "timestamp": "2020-04-23T05:53:04.898+0000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Paul Hix is not authorized",
  "path": "/api/v1/bridges/admin/config"
}

```


#### From Admin
```sh
   curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJSaWNrIiwicm9sZXMiOiJBZG1pbiJ9.hd2yFD_aEDL5IVaTz0TpiqxTTdZ4CteDLp1wquDTabI"\
          localhost:8080/api/v1/bridges/admin/config 
```



### Output 

```json
{
  "name": "Starter Config",
  "dateTime": "2020-04-22T21:49:55.292397",
  "bridges": [
    {
      "name": "jmsToNatsSample",
      "bridgeType": "REQUEST_REPLY",
      "source": {
        "name": "jms",
        "busType": "JMS",
        "config": {
          "type": "jms",
          "name": "ActiveMQ Sample",
          "config": {
            "java.naming.factory.initial": "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory",
            "connectionFactory.ConnectionFactory": "tcp://localhost:61616",
            "queue.queue/testQueue": "queue.queue/testQueue=testQueue"
          },
          "destinationName": "dynamicQueues/sample-jms-queue"
        }
      },
      "destination": {
        "name": "Nats Sample",
        "busType": "NATS",
        "config": {
          "type": "nats",
          "name": "nats",
          "host": "localhost",
          "port": 4222,
          "servers": [],
          "config": {},
          "subject": "sample-nats-subject"
        }
      },
      "copyHeaders": false
    },
    {
      "name": "jmsToNatsSample",
      "bridgeType": "REQUEST_REPLY",
      "source": {
        "name": "Source Nats Sample",
        "busType": "NATS",
        "config": {
          "type": "nats",
          "name": "activeMQ",
          "host": "localhost",
          "port": 4222,
          "servers": [],
          "config": {
            "java.naming.factory.initial": "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory",
            "connectionFactory.ConnectionFactory": "tcp://localhost:61616",
            "queue.queue/testQueue": "queue.queue/testQueue=testQueue"
          },
          "subject": "sample-nats-subject"
        }
      },
      "destination": {
        "name": "Destination JMS ActiveMQ",
        "busType": "JMS",
        "config": {
          "type": "jms",
          "name": "ActiveMQ Sample",
          "config": {
            "java.naming.factory.initial": "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory",
            "connectionFactory.ConnectionFactory": "tcp://localhost:61616",
            "queue.queue/testQueue": "queue.queue/testQueue=testQueue"
          },
          "destinationName": "dynamicQueues/sample-jms-queue"
        }
      },
      "copyHeaders": false
    }
  ]
}
```

## Show current user roles 

#### Paul
```sh
   curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJQYXVsIEhpeCIsInJvbGVzIjoiVXNlciJ9.Pb02geRU-RPyHyGwgxlwnptJ66zAs4nbdR7z53JY6RU"\
          localhost:8080/api/v1/logins/admin/roles 
```


#### Rick
```sh
   curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJSaWNrIiwicm9sZXMiOiJBZG1pbiJ9.hd2yFD_aEDL5IVaTz0TpiqxTTdZ4CteDLp1wquDTabI"\
          localhost:8080/api/v1/logins/admin/roles 
```




```sh
export TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMT0dJTl9UT0tFTiIsInN1YmplY3QiOiJhZG1pbiIsInJvbGVzIjoiQWRtaW4ifQ.UniWFiHOof9NkkMvV18Ch-r7Jm6UAcg1JfjrkUIWEk8

curl  -H "Authorization: Bearer $TOKEN"\
          localhost:8080/api/v1/bridges/admin/config | jq .

```

# Bridge Configuration Notes 

Note that many items have a name. It is important to give short concise names as they are used for reporting health issues, alerts, metrics and KPIs. 

The ***Nats JMS/MQ Bridge*** bridges message bus systems.
This allows sending and receiving messages between message bus systems like JMS and IBM MQ. 


## NatsBridgeConfig
 `NatsBridgeConfig` is the root config object for exporting to and reading from JSON and YAML.

Clusters refer to message bus servers for Nats or JMS/IBM MQ.
 
#### NatsBridgeConfig Schema 
```kotlin
data class NatsBridgeConfig(val name: String,
                            val bridges: List<MessageBridge>,
                            val clusters: Map<String, Cluster>)
```

#### NatsBridge in YAML 

```yaml 
name: "Starter Config"

bridges:
- name: "jmsToNatsSample"
  bridgeType: "REQUEST_REPLY"
  source:
    name: "jms"
    busType: "JMS"
    subject: "dynamicQueues/sample-jms-queue"
    clusterName: "activeMQTest"
  destination:
    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"
  copyHeaders: false
- name: "natsToJMS"
  bridgeType: "FORWARD"
  source:
    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"
  destination:
    name: "jms"
    busType: "JMS"
    subject: "dynamicQueues/sample-jms-queue"
    clusterName: "activeMQTest"
  copyHeaders: false
clusters:
  activeMQTest:
    name: "activeMQTest"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://localhost:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      jmsDestinationType: "QUEUE"
  natsTest:
    name: "natsTest"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config: {}
```

## MessageBus

A Message bus represents a message bus system, i.e., IBM MQ, Nats, ActiveMQ, JMS, Rabbit MQ, Kafka, SQS, etc.
A message bus has a subject which can be Nats subject or a JMS destination.

#### MessageBus Schema 

```kotlin
data class MessageBus(val name: String, val busType: BusType, val subject: String, val clusterName: String)
```

#### MessageBus In YAML

```yaml

    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"

```

## MessageBridge

A `MessageBridge` connects two MessageBus and will forward messages or relay request/replies between the message bus systems.
There is a source `MessageBus` and a destination `MessageBus`.

#### MessageBridge Schema 

```kotlin 
data class MessageBridge(val name: String, val bridgeType: BridgeType,
                         val source: MessageBus, val destination: MessageBus, val copyHeaders: Boolean? = false)
```

#### MessageBridge In YAML

```kotlin 
- name: "jmsToNatsSample" 
  bridgeType: "REQUEST_REPLY"
  source:
    name: "jms"
    busType: "JMS"
    subject: "dynamicQueues/sample-jms-queue"
    clusterName: "activeMQTest"
  destination:
    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"
  copyHeaders: false #should you copy headers between message busses
```


## BusType

#### BusType Schema 

```kotlin
enum class BusType { JMS, NATS }
```

#### BusType In YAML

```yaml
busType: "NATS"
```

Note that `busType` is an attribute of `MessageBus`. 

Nats JMS / IBM MQ Bridge currently supports two supported message bus types namely JMS (ActiveMQ and IBM MQ),
and NATS.

It is possible that this system could support Nats Streaming, Kinesis, Kafka, SQS, RabbitMQ, and more in the future.
 
Through JMS support, the Nats bridge could theoretically support the following JMS implementations:
* Amazon SQS's Java Messaging Library
* Apache ActiveMQ
* Apache Qpid, using AMQP
* IBM MQ (formerly MQSeries, then WebSphere MQ)
* IBM WebSphere Application Server's Service Integration Bus (SIBus)
* JBoss Messaging and HornetQ from JBoss
* JORAM from the OW2 Consortium
* Open Message Queue from Oracle
* OpenJMS from the OpenJMS Group
* Oracle WebLogic Server and Oracle AQ
* RabbitMQ from Pivotal Software
* SwiftMQ and others




## BridgeType
The two supported Bridge types are request/reply and forward to subject/destination (queue to queue).

#### BridgeType Schema 

```kotlin
enum class BridgeType { REQUEST_REPLY, FORWARD }
```
#### BridgeType In YAML

```yaml
  bridgeType: "REQUEST_REPLY"
```
Note that `bridgeType` is an attribute of `MessageBridge`. 

## Cluster
Cluster is a way to set up a server or groups of servers for a MessageBus like Nats, or IBM MQ.
This allows the easy configuration of bridges by moving the server/cluster configuration out of the bridge configuration code. 
Then the message buses (`MessageBus`es) in a bridge (`MessageBridge`) refer to the cluster name instead of inlining the server/cluster configuration for each bridge. 
  

#### Cluster Schema 

```kotlin
data class Cluster(val name: String?, val properties: ClusterConfig)

interface ClusterConfig {
    val config: Map<String, String>
    val userName: String?
    val password: String?
}

/**
 * Specific cluster config object for JMS.
 */
data class JmsClusterConfig(override val config: Map<String, String> = emptyMap(),
                            override val userName: String? = null, override val password: String? = null,
                            val jmsDestinationType: JmsDestinationType = JmsDestinationType.QUEUE) : ClusterConfig

/**
 * Specific cluster config object for NATS.
 */
data class NatsClusterConfig(override val userName: String? = null, override val password: String? = null,
                             val host: String? = null, val port: Int? = -1, val servers: List<String>? = emptyList(),
                             override val config: Map<String, String> = emptyMap()) : ClusterConfig


```

#### Cluster In YAML

#### ActiveMQ Cluster
```yaml
  activeMQTest:
    name: "activeMQTest"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://localhost:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      jmsDestinationType: "QUEUE"
```

#### Nats Cluster

```yaml 

  natsTest:
    name: "natsTest"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config: {}
```



# TSV File format for Bridges Config



To simplify set up, the bridge allows importing TSV files. 

You can import TSV files to configure the Nats JMS/MQ bridge. 

To import a group of configured bridges refer to the following example:

```tsv
# JMS to Nats Bridge
jms To Nats <TAB> r <TAB> jms Bar <TAB> j <TAB> queue/barQueue <TAB> activeMQTest<TAB> nats Foo <TAB> n <TAB> fooSubject<TAB> natsTest
# Nats To JMS Bridge
Nats To JMS <TAB> f <TAB> nat Bar <TAB> n <TAB> natsBarSubject <TAB> natsTest    <TAB> jms Foo  <TAB> j <TAB> queue/Foo <TAB> activeMQTest
```

1. Lines that begin with # are comments 
2. There will be exactly 10 entries per line that denote the config of two buses (`MessageBus`) that make up a bridge (`MessageBridge`)
3. Each line that is not a comment refers to a bridge (`MessageBridge`)
4. White space around the `<TAB>`s (refers to `\t`, i.e., `0x9`) is ignored


The format for the bridge line (`MessageBridge`) is as follows:

```tsv
BRIDGE_NAME <TAB> BRIDGE_TYPE <TAB> SOURCE_NAME <TAB> SOURCE_TYPE <TAB> SOURCE_SUBJECT <TAB> SOURCE_CLUSTER <TAB> DESTINATION_NAME  <TAB> DESTINATION_TYPE <TAB> DESTINATION_SUBJECT <TAB> DESTINATION_CLUSTER
```

#### Position numbers and allowed values for CSV file

```kotlin 
    private const val BRIDGE_NAME = 0
    private const val BRIDGE_TYPE = 1
    private const val SOURCE_NAME = 2
    private const val SOURCE_TYPE = 3
    private const val SOURCE_SUBJECT = 4
    private const val SOURCE_CLUSTER = 5
    private const val DESTINATION_NAME = 6
    private const val DESTINATION_TYPE = 7
    private const val DESTINATION_SUBJECT = 8
    private const val DESTINATION_CLUSTER = 9
    private const val BRIDGE_TYPE_REQUEST_REPLY = "r"
    private const val BRIDGE_TYPE_REQUEST_FORWARD = "f"
    private val VALID_BRIDGE_TYPES = setOf(BRIDGE_TYPE_REQUEST_FORWARD, BRIDGE_TYPE_REQUEST_REPLY)
    private const val BRIDGE_TYPE_JMS = "j"
    private const val BRIDGE_TYPE_NATS = "n"
    private val VALID_BUS_TYPES = setOf(BRIDGE_TYPE_JMS, BRIDGE_TYPE_NATS)

```

#### Each position explained.

```tsv

# Name of bridge  Request/reply or Forward
#                                                     JMS or NATS       Source Destination   Cluster Name                          
# 0 POS           1 POS (r or f)    2 POS             3 POS (j or n)    4 POS                5 POS                6 POS                   7 POS                  8 POS                     9 POS                          
BRIDGE_NAME <TAB> BRIDGE_TYPE <TAB> SOURCE_NAME <TAB> SOURCE_TYPE <TAB> SOURCE_SUBJECT <TAB> SOURCE_CLUSTER <TAB> DESTINATION_NAME  <TAB> DESTINATION_TYPE <TAB> DESTINATION_SUBJECT <TAB> DESTINATION_CLUSTER
```

#### Parsing a single TSV Line
```kotlin 

    fun parseLine(line: String, clusterConfigs: Map<String, Cluster>, delim: String="\t"): MessageBridge {
        val parts = line.split(delim).map{it.trim()}.filter { !it.isBlank() }.toList()
        if (parts.size != 10) throw BridgeDelimImporterException("Line must have ten cells but only has a size of ${parts.size}")
        val name = parts[BRIDGE_NAME]
        val sBridgeType = parts[BRIDGE_TYPE]
        val sourceName = parts[SOURCE_NAME]
        val sSourceType = parts[SOURCE_TYPE]
        val sourceSubject = parts[SOURCE_SUBJECT]
        val sourceClusterName = parts[SOURCE_CLUSTER]
        val destName = parts[DESTINATION_NAME]
        val sDestType = parts[DESTINATION_TYPE]
        val destSubject = parts[DESTINATION_SUBJECT]
        val destClusterName = parts[DESTINATION_CLUSTER]

        if (!VALID_BRIDGE_TYPES.contains(sBridgeType))
            throw BridgeDelimImporterException("bridge $name has illegal bridge type, " +
                    "$sBridgeType not in (r, f), $parts")
        if (!VALID_BUS_TYPES.contains(sSourceType))
            throw BridgeDelimImporterException("bridge $name has illegal source bus type, " +
                    "$sSourceType not in (n, j), $parts")
        if (!VALID_BUS_TYPES.contains(sDestType))
            throw BridgeDelimImporterException("bridge $name has illegal destination bus type, " +
                    "$sDestType not in (n, j), $parts")
        if (!clusterConfigs.containsKey(sourceClusterName))
            throw BridgeDelimImporterException("bridge $name has a source $sourceName for subject $sourceSubject " +
                    "cluster name $sourceClusterName that does not exist, $parts")
        if (!clusterConfigs.containsKey(destClusterName))
            throw BridgeDelimImporterException("bridge $name has a source $destName for subject $destSubject " +
                    "cluster name $destClusterName that does not exist, $parts")

        val bridgeType = if (sBridgeType == BRIDGE_TYPE_REQUEST_REPLY) BridgeType.REQUEST_REPLY else BridgeType.FORWARD
        val sourceType = if (sSourceType == BRIDGE_TYPE_JMS) BusType.JMS else BusType.NATS
        val destType = if (sDestType == BRIDGE_TYPE_JMS) BusType.JMS else BusType.NATS
        val sourceBus = MessageBus(name = sourceName, busType = sourceType, clusterName = sourceClusterName, subject = sourceSubject)
        val destBus = MessageBus(name = destName, busType = destType, clusterName = destClusterName, subject = destSubject)
        return MessageBridge(name = name, bridgeType = bridgeType, source = sourceBus, destination = destBus)
    }
```





