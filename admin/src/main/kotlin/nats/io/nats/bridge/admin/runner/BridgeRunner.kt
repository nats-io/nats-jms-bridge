package nats.io.nats.bridge.admin.runner


import io.nats.bridge.MessageBridge
import io.nats.bridge.MessageBus
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.NatsMessageBus
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.RepoException
import nats.io.nats.bridge.admin.models.bridges.*
import nats.io.nats.bridge.admin.runner.support.EndProcessSignal
import nats.io.nats.bridge.admin.runner.support.EndProcessSignalImpl
import nats.io.nats.bridge.admin.runner.support.SendEndProcessSignal
import nats.io.nats.bridge.admin.runner.support.SendEndProcessSignalImpl
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class BridgeRunnerException(message: String) : Exception(message)

object BridgeRunnerBuilder {
    var repo: ConfigRepo? = null
    var stop: AtomicBoolean? = null
    val endProcessSignalRef: AtomicReference<EndProcessSignal> = AtomicReference()
    val sendEndProcessSignalRef: AtomicReference<SendEndProcessSignal> = AtomicReference()

    fun endProcessSignal() = endProcessSignalRef.get()!!
    fun sendEndProcessSignal() = sendEndProcessSignalRef.get()!!


    fun build () : BridgeRunner {
        if (repo==null) throw BridgeRunnerException("Repo cannot be null")
        if (stop==null) {
            stop = AtomicBoolean()
        }

        val endProcessSignal : EndProcessSignal = EndProcessSignalImpl(stop!!)
        val sendEndProcessSignal : SendEndProcessSignal = SendEndProcessSignalImpl(stop!!)

        endProcessSignalRef.set(endProcessSignal)
        sendEndProcessSignalRef.set(sendEndProcessSignal)
        return BridgeRunner(repo!!, endProcessSignal, sendEndProcessSignal)
    }
}

class BridgeRunner(private val repo: ConfigRepo, val endProcessSignal : EndProcessSignal,
                   val sendEndProcessSignal : SendEndProcessSignal,
                   private val stopped : AtomicBoolean= AtomicBoolean(),
                   private val wasStarted : AtomicBoolean= AtomicBoolean()) {

    data class Details(val messageBridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)


    fun isStopped() = stopped.get()

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
            wasStarted.set(true)

            try {
                while (endProcessSignal.stopRunning()) {
                    messageBridge.process()
                    // TODO I need a way to chain a bunch of bridges and know if any had messages or requests to process
                    // Iterate through the list.
                    // If none had any messages than sleep for a beat.
                    // This has not been implemented yet
                    // This is a good place for a KPI metric as well
                    Thread.sleep(10)
                }
            }
            catch (ex:Exception) {
                stopped.set(true)
                ex.printStackTrace()
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

        sendEndProcessSignal.sendStopRunning()
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