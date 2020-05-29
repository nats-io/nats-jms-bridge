package io.nats.bridge.admin.runner.support

import io.nats.bridge.MessageBridge
import io.nats.bridge.MessageBridgeTasksManager
import io.nats.bridge.support.MessageBridgeBuilder
import io.nats.bridge.task.MessageBridgeTasksManagerBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


class BuilderTracker(val workers: Int, val tasks: Int, val builderList: List<MessageBridgeBuilder>) {
    var index: Int = 0

    fun build(): MessageBridge {
        if (builderList.size == 1 || (workers == 1 && tasks == 1)) {
            return builderList[0].build()
        } else {

            try {
                val build = builderList[index].build()
                index++
                return build
            } catch (ex: IndexOutOfBoundsException) {
                throw IllegalStateException("workers=$workers tasks=$tasks; total = ${workers * tasks}, but $index")
            }

        }
    }
}

class MessageBridgeRunner(private val messageBridgeLoader: MessageBridgeLoader) {


    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val managers: ConcurrentHashMap<String, MessageBridgeTasksManager> = ConcurrentHashMap()


    @PostConstruct
    fun init() {

        val loadBridgeConfig: List<BridgeConfig> = messageBridgeLoader.loadBridgeConfigs()
        val mapping: Map<String, MessageBridgeTasksManager> = loadBridgeConfig.map { bc ->
            val tracker = BuilderTracker(bc.config.workers ?: 1, bc.config.tasks ?: 1, bc.builders)
            MessageBridgeTasksManagerBuilder.builder().withName(bc.name)
                    .withWorkers(bc.config.workers ?:1).withTasks(bc.config.tasks ?: 1).withBridgeFactory {
                        tracker.build()
                    }.withPollDuration(Duration.ofMillis(50))

        }.map { it.name to it.build() }.toMap()
        managers.clear()
        managers.putAll(mapping)

        managers.forEach { (_, v) -> v.start() }

    }

    fun restart() {
        stop()
        init()
    }

    @PreDestroy
    fun stop() {
        managers.forEach { (name, v) ->
            try {
                v.close()
            } catch (ex: Exception) {
                logger.error("unable to stop bridge runner {}", name)
                logger.error("unable to stop bridge runner ", ex)
            }
        }
    }

    fun isHealthy(): Boolean = managers.values.all { it.isHealthy }
    fun isRunning(): Boolean = managers.values.all { it.isHealthy }
    fun wasStarted(): Boolean = managers.values.all { it.wasStarted() }
    fun wasError(): Boolean = managers.values.any { !it.isHealthy }
    fun getLastError(): Exception? = managers.values.find { it.lastError() != null }?.lastError()
    fun clearLastError() {
        managers.values.forEach { it.clearLastError() }
    }

}
