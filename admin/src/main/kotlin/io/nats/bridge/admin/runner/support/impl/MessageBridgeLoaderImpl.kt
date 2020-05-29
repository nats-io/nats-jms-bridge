package io.nats.bridge.admin.runner.support.impl


import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.models.bridges.*
import io.nats.bridge.admin.runner.support.BridgeConfig
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import io.nats.bridge.admin.util.getLogger
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import io.nats.bridge.support.MessageBridgeBuilder
import io.nats.bridge.support.MessageBusBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class MessageBridgeLoaderImpl(private val repo: ConfigRepo, private val metricsRegistry: MeterRegistry? = null) : MessageBridgeLoader {

    override fun loadBridgeConfigs() = doLoadMessageBridge(repo.readConfig())

    private val logger = LoggerFactory.getLogger(this.javaClass)

    data class Details(val bridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)

    private fun doLoadMessageBridge(config: NatsBridgeConfig) = config.bridges.map { bridge ->

        logger.info("Hello Rick")
        logger.trace("Hello Cowboy")
        getLogger("infolevel").info("Hello My Friend Rick")
        getLogger("tracelevel").trace("Hello My Friend Robert")
        val details = extractDetails(bridge, config)

        val list = if ((bridge.workers == 1 || bridge.workers == 0) && (bridge.tasks==1 || bridge.tasks==0)) {
            val bridgeBuilder = MessageBridgeBuilder()
            configureBridge(bridgeBuilder, details)
            listOf(bridgeBuilder)
        } else {
            (1..bridge.workers!!).map { workerNum ->
                (1..bridge.tasks!!).map { taskNum ->
                    val bridgeBuilder = MessageBridgeBuilder()
                    configureBridge(bridgeBuilder, details, "_w_${workerNum}_t_${taskNum}")
                    bridgeBuilder
                }
            }.flatten()
        }
        BridgeConfig(bridge.name, list, bridge)
    }

    private fun configureBridge(b: MessageBridgeBuilder, details: Details, postFix: String = "") {

        val dst = createMessageBusBuilder(details.bridge.destination, details.destinationCluster, details.bridge)
        val src = createMessageBusBuilder(details.bridge.source, details.sourceCluster, details.bridge)

        b.withDestinationBusBuilder(dst)
                .withSourceBusBuilder(src)
                .withRequestReply(details.bridge.bridgeType == BridgeType.REQUEST_REPLY)
                .withName(details.bridge.name + postFix)

        if (src is JMSMessageBusBuilder) {
            src.withName(src.name + postFix)
            src.asSource()
        } else if (src is NatsMessageBusBuilder) {
            src.withName(src.name + postFix)
        }
        if (dst is JMSMessageBusBuilder) {
            dst.withName(dst.name + postFix)
        } else if (dst is NatsMessageBusBuilder) {
            dst.withName(dst.name + postFix)
        }
    }

    private fun extractDetails(bridge: MessageBridgeInfo, config: NatsBridgeConfig) =
            Details(bridge, config.clusters[bridge.source.clusterName]
                    ?: error("${bridge.source.clusterName} not found"),
                    config.clusters[bridge.destination.clusterName]
                            ?: error("${bridge.destination.clusterName} not found"))


    private fun createMessageBusBuilder(busInfo: MessageBusInfo, cluster: Cluster, bridge: MessageBridgeInfo): MessageBusBuilder? {
        return if (busInfo.busType == BusType.NATS) {
            val natsClusterConfig = cluster.properties as NatsClusterConfig
            configureNatsBusBuilder(busInfo, bridge, natsClusterConfig)
        } else {
            val jmsClusterConfig = cluster.properties as JmsClusterConfig
            configureJmsBusBuilder(busInfo, bridge, jmsClusterConfig)
        }
    }

    private fun configureJmsBusBuilder(busInfo: MessageBusInfo, bridge: MessageBridgeInfo, config: JmsClusterConfig): MessageBusBuilder? {
        val builder = JMSMessageBusBuilder.builder()
                .withDestinationName(busInfo.subject).withName(busInfo.name)



        if (busInfo.responseSubject != null) {
            builder.withResponseDestinationName(busInfo.responseSubject)
        } else if  (bridge.bridgeType == BridgeType.FORWARD) {
            builder.withRequestReply(false)
        }

        if (metricsRegistry != null)
            builder.withMetricsProcessor(SpringMetricsProcessor(metricsRegistry, builder.metrics, 10,
                    Duration.ofSeconds(30), builder.timeSource, { builder.name }))

        if (config.userName != null) builder.withUserNameConnection(config.userName)
        if (config.password != null) builder.withPasswordConnection(config.password)
        if (bridge.copyHeaders != null) builder.withCopyHeaders(bridge.copyHeaders)

        if (config.autoConfig == JmsAutoConfig.IBM_MQ) {
            builder.useIBMMQ()
        } else if (config.autoConfig == JmsAutoConfig.ACTIVE_MQ) {
            builder.jndiProperties
        }

        if (config.config.isNotEmpty()) builder.withJndiProperties(config.config)


        return builder
    }

    private fun configureNatsBusBuilder(busInfo: MessageBusInfo, bridge: MessageBridgeInfo, clusterConfig: NatsClusterConfig): MessageBusBuilder? {
        val builder = NatsMessageBusBuilder.builder()
                .withSubject(busInfo.subject).withName(busInfo.name)

        if (metricsRegistry != null)
            builder.withMetricsProcessor(SpringMetricsProcessor(metricsRegistry, builder.metrics, 10,
                    Duration.ofSeconds(30), builder.timeSource, { builder.name }))

        val port = clusterConfig.port ?: 4222
        if (clusterConfig.host != null && port > 0) {
            builder.withHost(clusterConfig.host)
        }
        if (clusterConfig.servers != null && clusterConfig.servers.isNotEmpty()) {
            builder.withServers(clusterConfig.servers)
        }

        /* This is where we added SSL / TLS config into Nats Options Builder.*/
        if (clusterConfig.config.isNotEmpty()) {
            val props = Properties()
            props.putAll(clusterConfig.config)
            builder.withOptionProperties(props)
        }

        if (clusterConfig.userName != null && clusterConfig.password != null) {
            builder.withPassword(clusterConfig.password).withUser(clusterConfig.userName)
        }

        return builder
    }

}