package nats.io.nats.bridge.admin.runner.support.impl

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import io.nats.bridge.support.MessageBusBuilder
import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.*
import nats.io.nats.bridge.admin.runner.support.MessageBridgeBuilder
import nats.io.nats.bridge.admin.runner.support.MessageBridgeLoader
import java.time.Duration
import java.util.*

class MessageBridgeLoaderImpl(private val repo: ConfigRepo, private val metricsRegistry: MeterRegistry? = null) : MessageBridgeLoader {


    override fun loadBridgeBuilders(): List<MessageBridgeBuilder> = doLoadMessageBridge(repo.readConfig())

    data class Details(val messageBridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)

    private fun doLoadMessageBridge(config: NatsBridgeConfig) = config.bridges.flatMap { bridge ->
        val details = extractDetails(bridge, config)

        if (bridge.workers == 1 || bridge.workers == 0) {
            listOf(MessageBridgeBuilder()
                    .withDestinationBusBuilder(createMessageBusBuilder(bridge.destination, details.destinationCluster, bridge))
                    .withSourceBusBuilder(createMessageBusBuilder(bridge.source, details.sourceCluster, bridge))
                    .withRequestReply(bridge.bridgeType == BridgeType.REQUEST_REPLY)
                    .withName(bridge.name))
        } else {
            (1..bridge.workers!!).map { bridgeNum ->

                val b = MessageBridgeBuilder()
                        .withDestinationBusBuilder(createMessageBusBuilder(bridge.destination, details.destinationCluster, bridge))
                        .withSourceBusBuilder(createMessageBusBuilder(bridge.source, details.sourceCluster, bridge))
                        .withRequestReply(bridge.bridgeType == BridgeType.REQUEST_REPLY)
                        .withName(bridge.name + bridgeNum)

                val src = b.sourceBusBuilder
                val dst = b.destBusBuilder
                if (src is JMSMessageBusBuilder) {
                    src.withName(src.name + bridgeNum)
                    src.asSource()
                } else if (src is NatsMessageBusBuilder) {
                    src.withName(src.name + bridgeNum)
                }
                if (dst is JMSMessageBusBuilder) {
                    dst.withName(dst.name + bridgeNum)
                } else if (dst is NatsMessageBusBuilder) {
                    dst.withName(dst.name + bridgeNum)
                }
                b

            }

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

        if (metricsRegistry != null)
            builder.withMetricsProcessor(SpringMetricsProcessor(metricsRegistry, builder.metrics, 10,
                    Duration.ofSeconds(30), builder.timeSource, {builder.name}))

        if (config.userName != null) {
            builder.withUserNameConnection(config.userName)
        }
        if (config.password != null) {
            builder.withPasswordConnection(config.password)
        }
        if (bridge.copyHeaders != null) {
            builder.withCopyHeaders(bridge.copyHeaders)
        }
        if (config.config.isNotEmpty()) {
            builder.withJndiProperties(config.config)
        }
        return builder
    }

    private fun configureNatsBusBuilder(busInfo: MessageBusInfo, bridge: MessageBridgeInfo, clusterConfig: NatsClusterConfig): MessageBusBuilder? {
        val builder = NatsMessageBusBuilder.builder()
                .withSubject(busInfo.subject).withName(busInfo.name)

        if (metricsRegistry != null)
            builder.withMetricsProcessor(SpringMetricsProcessor(metricsRegistry, builder.metrics, 10,
                    Duration.ofSeconds(30), builder.timeSource,{builder.name}))

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