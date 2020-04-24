package nats.io.nats.bridge.admin.runner

import io.nats.bridge.MessageBridge
import io.nats.bridge.MessageBus
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.NatsMessageBus
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.*
import nats.io.nats.bridge.admin.runner.support.EndProcessSignal
import nats.io.nats.bridge.admin.runner.support.EndProcessSignalImpl
import nats.io.nats.bridge.admin.runner.support.SendEndProcessSignal
import nats.io.nats.bridge.admin.runner.support.SendEndProcessSignalImpl
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


class BridgeRunnerException(message: String) : Exception(message)


class BridgeRunnerBuilder {
    var repo: ConfigRepo? = null
        private set
    var stop: AtomicBoolean? = AtomicBoolean()
        private set
    var endProcessSignal: EndProcessSignal = EndProcessSignalImpl(stop!!)
        private set
    var sendEndProcessSignal: SendEndProcessSignal = SendEndProcessSignalImpl(stop!!)
        private set

    var messageLoader : MessageBridgeLoader? = null
        private set

    fun withMessageBridgeLoader(messageLoader : MessageBridgeLoader) = apply{ this.messageLoader = messageLoader}


    fun withRepo(repo: ConfigRepo) = apply { this.repo = repo }
    fun withSignal(stop: AtomicBoolean) = apply {
        this.stop = stop
        endProcessSignal = EndProcessSignalImpl(stop)
        sendEndProcessSignal = SendEndProcessSignalImpl(stop)
    }

    fun build(): BridgeRunner {


        if (messageLoader == null && repo == null ) throw BridgeRunnerException("Message Bridge Loader cannot be null")

        if (messageLoader == null) {
            withRepo(repo!!)
        }

        return BridgeRunner(messageLoader!!, endProcessSignal, sendEndProcessSignal)
    }
}


class BridgeRunnerManager(private val repo: ConfigRepo,
                          private val stopSignal: AtomicBoolean = AtomicBoolean(),
                          private val bridgeRunnerRef: AtomicReference<BridgeRunner> = AtomicReference()) {

    fun endProcessSignal() = endProcessSignalRef.get()!!
    fun sendEndProcessSignal() = sendEndProcessSignalRef.get()!!

    @PostConstruct
    fun init() = bridgeRunner().initRunner()

    fun restart() {
        stop()
        /* Force Bridge to reload its configuration. */
        bridgeRunnerRef.set(null)
        init()
    }

    @PreDestroy
    fun stop() {
        if (!bridgeRunner().isStopped()) {
            bridgeRunner().stopRunner()
        }
    }

    private val endProcessSignalRef: AtomicReference<EndProcessSignal> = AtomicReference()
    private val sendEndProcessSignalRef: AtomicReference<SendEndProcessSignal> = AtomicReference()
    private fun bridgeRunner(): BridgeRunner {
        if (bridgeRunnerRef.get() == null) {
            val builder = BridgeRunnerBuilder()
            builder.withRepo(repo).withSignal(stopSignal)
            bridgeRunnerRef.compareAndSet(null, builder.build())
        }
        return bridgeRunnerRef.get()!!
    }

}

interface MessageBridgeLoader {
    fun loadBridges(): List<MessageBridge>
}

class MessageBridgeLoaderImpl(private val repo: ConfigRepo) {

    fun loadBridges(): List<MessageBridge> = listOf(loadMessageBridge())

    private fun loadMessageBridge(): MessageBridge {
        val config = repo.readConfig()
        val bridge = config.bridges[0]
        val details = BridgeRunner.Details(bridge, config.clusters[bridge.source.clusterName]!!, config.clusters[bridge.destination.clusterName]!!)
        val sourceBus = createMessageBus(bridge.source, details.sourceCluster, bridge)!!
        val destinationBus = createMessageBus(bridge.destination, details.destinationCluster, bridge)!!
        val messageBridge = MessageBridge(sourceBus, destinationBus, bridge.bridgeType == BridgeType.REQUEST_REPLY)
        return messageBridge
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

class BridgeRunner(private val bridgeLoader: MessageBridgeLoader,
                   private val endProcessSignal: EndProcessSignal,
                   private val sendEndProcessSignal: SendEndProcessSignal,
                   private val stopped: AtomicBoolean = AtomicBoolean(),
                   private val wasStarted: AtomicBoolean = AtomicBoolean()) {
    data class Details(val messageBridge: MessageBridgeInfo, val sourceCluster: Cluster, val destinationCluster: Cluster)

    private val lastErrorRef = AtomicReference<Exception>()
    private var executors = Executors.newFixedThreadPool(1)

    fun wasError() = lastErrorRef.get() != null
    fun getLastError() = lastErrorRef.get()!!
    fun clearLastError() = lastErrorRef.set(null)
    fun isStopped() = stopped.get()
    fun stopRunner() = sendEndProcessSignal.sendStopRunning()


    fun initRunner() {
        val messageBridge = loadMessageBridge()


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
                    Thread.sleep(100)//Temp hack to test
                }
                stopped.set(true)
            } catch (ex: Exception) {
                stopped.set(true)
                ex.printStackTrace()
            }
        })
    }

    private fun loadMessageBridge(): MessageBridge = bridgeLoader.loadBridges()[0]


}