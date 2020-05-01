package io.nats.bridge.admin.runner

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.MessageBridge
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.runner.support.BridgeRunnerBuilder
import io.nats.bridge.admin.runner.support.EndProcessSignal
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import io.nats.bridge.admin.runner.support.SendEndProcessSignal
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


class BridgeRunnerManager(private val repo: ConfigRepo,
                          private val bridgeRunnerRef: AtomicReference<BridgeRunner> = AtomicReference(),
                          private val metricsRegistry: MeterRegistry? = null) {

    fun endProcessSignal() = endProcessSignalRef.get()!!
    fun sendEndProcessSignal() = sendEndProcessSignalRef.get()!!
    private val lastErrorRef = AtomicReference<Exception>()
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @PostConstruct
    fun init() {
        try {
            bridgeRunner().initRunner()
        } catch (ex: Exception) {
            lastErrorRef.set(ex)
            logger.error("Unable to start bridge runner", ex)
        }
    }

    fun restart() {
        stop()
        /* Force Bridge to reload its configuration. */
        bridgeRunnerRef.set(null)
        lastErrorRef.set(null)
        init()
    }

    fun wasError() = lastErrorRef.get() !=null || bridgeRunner().wasError()

    fun wasStarted() = bridgeRunner().wasStarted()
    fun getLastError() : Exception? = bridgeRunner().getLastError()?:lastErrorRef.get()
    fun isRunning() = bridgeRunner().isRunning()
    fun isStopped() = bridgeRunner().isStopped()
    fun clearLastError() {
        bridgeRunner().clearLastError()
        lastErrorRef.set(null)
    }

    @PreDestroy
    fun stop() {
        if (bridgeRunner().isRunning()) {
            bridgeRunner().stopRunner()
        }
    }

    private val endProcessSignalRef: AtomicReference<EndProcessSignal> = AtomicReference()
    private val sendEndProcessSignalRef: AtomicReference<SendEndProcessSignal> = AtomicReference()

    private fun bridgeRunner(): BridgeRunner {
        if (bridgeRunnerRef.get() == null) {
            val builder = BridgeRunnerBuilder()

            builder.withRepo(repo)
            builder.withMessageBridgeLoader(MessageBridgeLoaderImpl(repo, metricsRegistry))

            if (bridgeRunnerRef.compareAndSet(null, builder.build())) {
                endProcessSignalRef.set(builder.endProcessSignal)
                sendEndProcessSignalRef.set(builder.sendEndProcessSignal)
            }
        }
        return bridgeRunnerRef.get()!!
    }


}

class BridgeRunner(private val bridgeLoader: MessageBridgeLoader,
                   private val endProcessSignal: EndProcessSignal,
                   private val sendEndProcessSignal: SendEndProcessSignal,
                   private val stopped: AtomicBoolean = AtomicBoolean(),
                   private val wasStarted: AtomicBoolean = AtomicBoolean(),
                   private val duration: Duration) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val lastErrorRef = AtomicReference<Exception>()
    private var executors: ExecutorService? = null

    fun wasError() = lastErrorRef.get() != null
    fun getLastError(): Exception? = lastErrorRef.get()
    fun clearLastError() = lastErrorRef.set(null)
    fun isStopped() = stopped.get()
    fun isRunning() = !isStopped()
    fun wasStarted() = wasStarted.get()

    fun stopRunner() {
        sendEndProcessSignal.sendStopRunning()
        executors?.shutdown()
        executors = null

    }


    fun initRunner() {
        val messageBridges = loadMessageBridges()
        executors = Executors.newFixedThreadPool(1)

        executors?.submit(Runnable {
            wasStarted.set(true)
            try {
                while (endProcessSignal.keepRunning()) {
                    val count = messageBridges.map { messageBridge -> messageBridge.process() }.sum()
                    if (count == 0) {
                        messageBridges[0].process(duration)
                        messageBridges.subList(1, messageBridges.size).forEach { it.process() }
                    }
                }
                stopped.set(true)
                logger.info("Stopped bridge runner")
            } catch (ex: Exception) {
                stopped.set(true)
                lastErrorRef.set(ex)
                logger.warn("Stopped bridge runner with error", ex)

            }
            messageBridges.forEach { mb ->
                try {
                    mb.close()
                } catch (ex: Exception) {
                    logger.warn("error shutting down bridge ${mb.name()}", ex)
                }
            }
        })
    }

    private fun loadMessageBridges(): List<MessageBridge> = bridgeLoader.loadBridges()


}