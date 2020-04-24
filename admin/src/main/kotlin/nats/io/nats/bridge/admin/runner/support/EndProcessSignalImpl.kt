package nats.io.nats.bridge.admin.runner.support

import java.util.concurrent.atomic.AtomicBoolean

class EndProcessSignalImpl(private val atomicBoolean : AtomicBoolean = AtomicBoolean()) : EndProcessSignal {
    override fun stopRunning() = atomicBoolean.get()
}