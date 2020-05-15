package io.nats.bridge.admin.runner.support

import io.nats.bridge.MessageBridgeTasksManager
import io.nats.bridge.task.MessageBridgeTasksManagerBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class MessageBridgeRunner(private val messageBridgeLoader: MessageBridgeLoader) {


    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val managers : ConcurrentHashMap<String, MessageBridgeTasksManager> = ConcurrentHashMap()


    @PostConstruct
    fun init() {

        val loadBridgeBuilders: List<BridgeConfig> = messageBridgeLoader.loadBridgeConfigs()
        val mapping: Map<String, MessageBridgeTasksManager> = loadBridgeBuilders.mapIndexed { index, bc ->
            MessageBridgeTasksManagerBuilder.builder().withName(bc.name)
                    .withWorkers(bc.config.workers ?: 5).withTasks(bc.config.tasks ?: 1).withBridgeFactory {
                        bc.builders[index].build()
                    }.withPollDuration(Duration.ofMillis(50))
        }.map { it.name to it.build() }.toMap()
        managers.clear()
        managers.putAll(mapping)

        managers.forEach{ (_, v) -> v.start()}

    }

    fun restart() {
        stop()
        init()
    }

    @PreDestroy
    fun stop() {
        managers.forEach{ (name, v) ->
            try {
                v.close()
            } catch (ex : Exception){
                logger.error("unable to stop bridge runner {}", name)
                logger.error("unable to stop bridge runner ", ex)
            }}
    }

    fun isHealthy(): Boolean = managers.values.all { it.isHealthy }
    fun isRunning(): Boolean = managers.values.all { it.isHealthy }
    fun wasStarted(): Boolean = managers.values.all { it.wasStarted() }
    fun wasError(): Boolean = managers.values.any{ !it.isHealthy }
    fun getLastError(): Exception? = managers.values.find { it.lastError()!=null }?.lastError()
    fun clearLastError() {
        managers.values.forEach{it.clearLastError()}
    }

}
