package nats.io.nats.bridge.admin.runner.support

import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.runner.BridgeRunner
import nats.io.nats.bridge.admin.runner.support.impl.EndProcessSignalImpl
import nats.io.nats.bridge.admin.runner.support.impl.SendEndProcessSignalImpl
import java.util.concurrent.atomic.AtomicBoolean

class BridgeRunnerBuilder {
    var repo: ConfigRepo? = null
        private set
    var stop: AtomicBoolean? = AtomicBoolean()
        private set
    var endProcessSignal: EndProcessSignal = EndProcessSignalImpl(stop!!)
        private set
    var sendEndProcessSignal: SendEndProcessSignal = SendEndProcessSignalImpl(stop!!)
        private set

    var messageLoader: MessageBridgeLoader? = null
        private set

    fun withMessageBridgeLoader(messageLoader: MessageBridgeLoader) = apply { this.messageLoader = messageLoader }


    fun withRepo(repo: ConfigRepo) = apply { this.repo = repo }
    fun withSignal(stop: AtomicBoolean) = apply {
        this.stop = AtomicBoolean()
        endProcessSignal = EndProcessSignalImpl(stop)
        sendEndProcessSignal = SendEndProcessSignalImpl(stop)
    }

    fun build(): BridgeRunner {


        if (messageLoader == null && repo == null) throw BridgeRunnerException("Message Bridge Loader cannot be null")

        if (messageLoader == null) {
            withRepo(repo!!)
        }

        return BridgeRunner(messageLoader!!, endProcessSignal, sendEndProcessSignal)
    }
}