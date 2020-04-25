package nats.io.nats.bridge.admin.runner.support.impl

import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import io.nats.bridge.support.MessageBusBuilder
import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.*
import nats.io.nats.bridge.admin.runner.support.MessageBridgeBuilder
import nats.io.nats.bridge.admin.runner.support.MessageBridgeLoader
import java.util.*

class MessageBridgeLoaderImpl(private val repo: ConfigRepo) : MessageBridgeLoader {


    override fun loadBridgeBuilders(): List<MessageBridgeBuilder> = doLoadMessageBridge(repo.readConfig())

    data class Details(val messageBridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)

    private fun doLoadMessageBridge(config: NatsBridgeConfig) = config.bridges.map { bridge ->
        val details = extractDetails(bridge, config)
        MessageBridgeBuilder()
                .withDestinationBusBuilder(createMessageBusBuilder(bridge.destination, details.destinationCluster, bridge))
                .withSourceBusBuilder(createMessageBusBuilder(bridge.source, details.sourceCluster, bridge))
                .withRequestReply(bridge.bridgeType == BridgeType.REQUEST_REPLY)
                .withName(bridge.name)
    }

    private fun extractDetails(bridge: MessageBridgeInfo, config: NatsBridgeConfig) =
            Details(bridge, config.clusters[bridge.source.clusterName]
                    ?: error("${bridge.source.clusterName} not found"),
                    config.clusters[bridge.destination.clusterName]
                            ?: error("${bridge.destination.clusterName} not found"))


    private fun createMessageBusBuilder(busInfo: MessageBusInfo, cluster: Cluster, bridge: MessageBridgeInfo): MessageBusBuilder? {
        return if (busInfo.busType == BusType.NATS) {
            val natsClusterConfig = cluster.properties as NatsClusterConfig
            configureNatsBusBuilder(bridge, natsClusterConfig)
        } else {
            val jmsClusterConfig = cluster.properties as JmsClusterConfig
            configureJmsBusBuilder(busInfo, jmsClusterConfig, bridge)
        }
    }

    private fun configureJmsBusBuilder(busInfo: MessageBusInfo, jmsClusterConfig: JmsClusterConfig, bridge: MessageBridgeInfo): MessageBusBuilder? {
        val builder = JMSMessageBusBuilder.builder().withDestinationName(busInfo.subject)
        if (jmsClusterConfig.userName != null) {
            builder.withUserNameConnection(jmsClusterConfig.userName)
        }
        if (jmsClusterConfig.password != null) {
            builder.withPasswordConnection(jmsClusterConfig.password)
        }
        if (bridge.copyHeaders != null) {
            builder.withCopyHeaders(bridge.copyHeaders)
        }
        if (jmsClusterConfig.config.isNotEmpty()) {
            builder.withJndiProperties(jmsClusterConfig.config)
        }

        return builder
    }

    private fun configureNatsBusBuilder(bridge: MessageBridgeInfo, clusterConfig: NatsClusterConfig): MessageBusBuilder? {
        val busInfo = bridge.destination
        val builder = NatsMessageBusBuilder.builder().withSubject(busInfo.subject)
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