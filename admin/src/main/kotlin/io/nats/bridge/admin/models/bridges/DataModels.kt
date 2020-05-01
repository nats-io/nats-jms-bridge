package io.nats.bridge.admin.models.bridges


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

/**
 * Root config object for exporting to and reading from JSON and YAML.
 * Clusters refer to message bus servers for Nats or JMS/IBM MQ.
 */
data class NatsBridgeConfig(val name: String,
                            val dateTime: LocalDateTime = LocalDateTime.now(),
                            val secretKey: String? = null,
                            val bridges: List<MessageBridgeInfo>,
                            val clusters: Map<String, Cluster>)


/**
 * A Message bus represents a message bus system, i.e., IBM MQ, Nats, ActiveMQ, JMS, Rabbit MQ, Kafka, SQS, etc.
 * A message bus has a subject which can be Nats subject or a JMS destination.
 */
data class MessageBusInfo(val name: String, val busType: BusType, val subject: String, val responseSubject: String?=null,
                          val clusterName: String)

/**
 * A Message Bridge connects two MessageBus and will forward messages or relay request replies between the message bus systems.
 *
 */
data class MessageBridgeInfo(val name: String, val bridgeType: BridgeType,
                             val source: MessageBusInfo, val destination: MessageBusInfo, val copyHeaders: Boolean? = false,
                             val workers: Int? = 1)

/**
 * Two supported message bus types are JMS and NATS.
 * This could support Nats Streaming, Kinesis, Kafka, SQS, RabbitMQ, etc.
 *
 * Through JMS support, it supports:
 *      Amazon SQS's Java Messaging Library
 *      Apache ActiveMQ
 *      Apache Qpid, using AMQP[17]
 *      IBM MQ (formerly MQSeries, then WebSphere MQ)
 *      IBM WebSphere Application Server's Service Integration Bus (SIBus)[18]
 *      JBoss Messaging and HornetQ from JBoss
 *      JORAM from the OW2 Consortium
 *      Open Message Queue from Oracle
 *      OpenJMS from the OpenJMS Group
 *      Oracle WebLogic Server and Oracle AQ
 *      RabbitMQ from Pivotal Software
 *      SwiftMQ and others
 */
enum class BusType { JMS, NATS }

/** Two Supported Bridge types are request/reply and
 *  forward to subject/destination (queue to queue).
 */
enum class BridgeType { REQUEST_REPLY, FORWARD }


/** Cluster config is a way to set up servers for a MessageBus like Nats, or IBM MQ. */
data class Cluster(val name: String?, val properties: ClusterConfig)

/**
 * Cluster
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Type(value = JmsClusterConfig::class, name = "jms"), Type(value = NatsClusterConfig::class, name = "nats"))
interface ClusterConfig {
    val config: Map<String, String>
    val userName: String?
    val password: String?
}

/**
 * JMS Destination type specifies a queue or a topic.
 * Currently, only queue support is in this release.
 */
enum class JmsDestinationType { QUEUE, TOPIC }

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


/** Used to generate initial config file examples. */
val defaultDataModel = NatsBridgeConfig(
        name = "Starter Config",
        bridges = listOf(
                MessageBridgeInfo("jmsToNatsSample",
                        bridgeType = BridgeType.REQUEST_REPLY,
                        workers = 5,
                        source = MessageBusInfo(name = "jms",
                                busType = BusType.JMS,
                                subject = "dynamicQueues/a-queue",
                                clusterName = "jmsCluster"
                        ),
                        destination = MessageBusInfo(name = "nats",
                                busType = BusType.NATS,
                                subject = "a-subject",
                                clusterName = "natsCluster"
                        )
                ),
                MessageBridgeInfo("natsToJMS",
                        workers = 5,
                        bridgeType = BridgeType.REQUEST_REPLY,
                        source = MessageBusInfo(name = "nats",
                                busType = BusType.NATS,
                                subject = "b-subject",
                                clusterName = "natsCluster"
                        ),
                        destination = MessageBusInfo(name = "jms",
                                busType = BusType.JMS,
                                subject = "dynamicQueues/b-queue",
                                clusterName = "jmsCluster"
                        )
                ),
                MessageBridgeInfo("ibmMqToNatsSample",
                        workers = 5,
                        bridgeType = BridgeType.REQUEST_REPLY,
                        source = MessageBusInfo(name = "ibmMQ",
                                busType = BusType.JMS,
                                subject = "DEV.QUEUE.1",
                                clusterName = "ibmMqCluster"
                        ),
                        destination = MessageBusInfo(name = "nats",
                                busType = BusType.NATS,
                                subject = "a-subject",
                                clusterName = "natsCluster"
                        )
                ),
                MessageBridgeInfo("natsToIBMMq",
                        workers = 5,
                        bridgeType = BridgeType.REQUEST_REPLY,
                        source = MessageBusInfo(name = "nats",
                                busType = BusType.NATS,
                                subject = "b-subject",
                                clusterName = "natsCluster"
                        ),
                        destination = MessageBusInfo(name = "ibmMQ",
                                busType = BusType.JMS,
                                subject = "DEV.QUEUE.1",
                                clusterName = "ibmMqCluster",
                                responseSubject = "DEV.QUEUE.2"
                        )
                )
        ),
        clusters = mapOf(
                "jmsCluster" to Cluster(
                        name = "jmsCluster",
                        properties = JmsClusterConfig(
                                userName = "cloudurable",
                                password = "cloudurable",
                                config = mapOf("java.naming.factory.initial" to
                                        "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory",
                                        "connectionFactory.ConnectionFactory" to "tcp://localhost:61616",
                                        "queue.queue/testQueue" to "queue.queue/testQueue=testQueue"
                                )
                        )
                ),
                "natsCluster" to Cluster(
                        name = "natsCluster",

                        properties = NatsClusterConfig(
                                host = "localhost",
                                port = 4222
                        )
                ),
                "ibmMqCluster" to Cluster(
                        name = "ibmMqCluster",
                        properties = JmsClusterConfig(
                                userName = "app",
                                password = "passw0rd",
                                config = mapOf(
                                        "java.naming.factory.initial" to "io.nats.bridge.ibmmq.IbmMqInitialContextFactory",
                                        "nats.ibm.mq.host" to "tcp://localhost:1414",
                                        "nats.ibm.mq.channel" to "DEV.APP.SVRCONN",
                                        "nats.ibm.mq.queueManager" to "QM1"
                                )

                        )
                )
        )

)