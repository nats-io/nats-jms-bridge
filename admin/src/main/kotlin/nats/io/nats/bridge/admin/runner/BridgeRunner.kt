package nats.io.nats.bridge.admin.runner


import io.nats.bridge.MessageBridge
import io.nats.bridge.MessageBus
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.NatsMessageBus
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BridgeRunner(private val repo: ConfigRepo) {

    data class Details(val messageBridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)


    fun initRunner() {

        val config = repo.readConfig()

        val bridge = config.bridges[0]


        val details = Details(bridge, config.clusters[bridge.source.clusterName]!!, config.clusters[bridge.destination.clusterName]!!)


        val sourceBus = createMessageBus(bridge.source, details.sourceCluster, bridge)!!

        val destinationBus = createMessageBus(bridge.destination, details.destinationCluster, bridge)!!


        val messageBridge = MessageBridge(sourceBus, destinationBus, bridge.bridgeType == BridgeType.REQUEST_REPLY)

        val executors = Executors.newFixedThreadPool(1)

        //Working on this, TODO flag it to stop
        executors.submit(Runnable {
            Thread.sleep(10)

            while (true) {
                messageBridge.process()
                Thread.sleep(10)
            }
        })


    }

    private fun createMessageBus(busInfo: MessageBusInfo, cluster: Cluster, bridge: MessageBridgeInfo): MessageBus? {
        return if (busInfo.busType == BusType.NATS) {
            val natsClusterConfig = cluster.properties as NatsClusterConfig
            configureNatsBus(bridge, natsClusterConfig)
        } else {
            val jmsClusterConfig = cluster.properties as JmsClusterConfig
            configureJmsBus(busInfo, jmsClusterConfig, bridge)
        }
    }

    fun stopRunner() {

    }

    private fun configureJmsBus(busInfo: MessageBusInfo, jmsClusterConfig: JmsClusterConfig, bridge: MessageBridgeInfo): MessageBus {
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

        return builder.build()
    }

    private fun configureNatsBus(bridge: MessageBridgeInfo, natsClusterConfig: NatsClusterConfig): NatsMessageBus? {
        val busInfo = bridge.source

        val builder = NatsMessageBusBuilder.builder().withSubject(busInfo.subject)

        val port = natsClusterConfig.port ?: 4222
        if (natsClusterConfig.host != null && port > 0) {
            builder.withHost(natsClusterConfig.host)
        }
        if (natsClusterConfig.servers != null && natsClusterConfig.servers.isNotEmpty()) {
            builder.withServers(natsClusterConfig.servers)
        }
        return builder.build()
    }


}