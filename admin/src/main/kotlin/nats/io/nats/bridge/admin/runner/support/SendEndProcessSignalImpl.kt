package nats.io.nats.bridge.admin.runner.support

import java.util.concurrent.atomic.AtomicBoolean

class SendEndProcessSignalImpl (private val atomicBoolean : AtomicBoolean = AtomicBoolean()) : SendEndProcessSignal {
    override fun sendStopRunning() = atomicBoolean.set(true)
}