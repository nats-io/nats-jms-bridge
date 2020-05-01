package io.nats.bridge.admin.runner.support.impl

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import io.nats.bridge.support.MessageBusBuilder
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.models.bridges.*
import io.nats.bridge.admin.runner.support.MessageBridgeBuilder
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import java.time.Duration
import java.util.*

class MessageBridgeLoaderImpl(private val repo: ConfigRepo, private val metricsRegistry: MeterRegistry? = null) : MessageBridgeLoader {


    override fun loadBridgeBuilders(): List<MessageBridgeBuilder> = doLoadMessageBridge(repo.readConfig())

    data class Details(val bridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)

    private fun doLoadMessageBridge(config: NatsBridgeConfig) = config.bridges.flatMap { bridge ->
        val details = extractDetails(bridge, config)
        if (bridge.workers == 1 || bridge.workers == 0) {
            val b = MessageBridgeBuilder()
            configureBridge(b, details, b.sourceBusBuilder, b.destBusBuilder)
            listOf(b)
        } else {
            (1..bridge.workers!!).map { bridgeNum ->
                val b = MessageBridgeBuilder()
                configureBridge(b, details, b.sourceBusBuilder, b.destBusBuilder, "_$bridgeNum")
                b
            }
        }
    }

    private fun configureBridge(b: MessageBridgeBuilder, details: Details, src: MessageBusBuilder?, dst: MessageBusBuilder?, postFix: String = "") {

        val d = createMessageBusBuilder(details.bridge.destination, details.destinationCluster, details.bridge)
        val s =createMessageBusBuilder(details.bridge.source, details.sourceCluster, details.bridge)

        b.withDestinationBusBuilder(d)
                .withSourceBusBuilder(s)
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

        if (busInfo.responseSubject!=null) builder.withResponseDestinationName(busInfo.responseSubject)

        if (metricsRegistry != null)
            builder.withMetricsProcessor(SpringMetricsProcessor(metricsRegistry, builder.metrics, 10,
                    Duration.ofSeconds(30), builder.timeSource, { builder.name }))

        if (config.userName != null) builder.withUserNameConnection(config.userName)
        if (config.password != null) builder.withPasswordConnection(config.password)
        if (bridge.copyHeaders != null) builder.withCopyHeaders(bridge.copyHeaders)
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