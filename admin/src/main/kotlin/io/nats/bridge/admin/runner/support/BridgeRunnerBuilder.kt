package io.nats.bridge.admin.runner.support

import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.runner.BridgeRunner
import io.nats.bridge.admin.runner.support.impl.EndProcessSignalImpl
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import io.nats.bridge.admin.runner.support.impl.SendEndProcessSignalImpl
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class BridgeRunnerBuilder {
    var repo: ConfigRepo? = null
        private set
    var stop: AtomicBoolean? = null
        private set

    var duration: Duration? = null
        private set

    var endProcessSignal: EndProcessSignal? = null
        private set
    var sendEndProcessSignal: SendEndProcessSignal? = null
        private set

    var messageLoader: MessageBridgeLoader? = null
        private set

    fun withMessageBridgeLoader(messageLoader: MessageBridgeLoader) = apply { this.messageLoader = messageLoader }


    fun withRepo(repo: ConfigRepo) = apply {
        this.repo = repo
    }

    fun withSignal(stop: AtomicBoolean) = apply {
        this.stop = AtomicBoolean()
        endProcessSignal = EndProcessSignalImpl(stop)
        sendEndProcessSignal = SendEndProcessSignalImpl(stop)
    }

    fun build(): BridgeRunner {


        if (stop == null) {
            withSignal(AtomicBoolean())
        }

        if (messageLoader == null && repo == null) throw BridgeRunnerException("Message Bridge Loader cannot be null")

        if (messageLoader == null) {
            withRepo(repo!!)
            messageLoader = MessageBridgeLoaderImpl(repo!!)
        }


        return BridgeRunner(messageLoader!!, endProcessSignal!!, sendEndProcessSignal!!, duration = duration
                ?: Duration.ofMillis(100))
    }
}