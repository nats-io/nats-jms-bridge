package nats.io.nats.bridge.admin.runner.support.impl

import nats.io.nats.bridge.admin.runner.support.EndProcessSignal
import java.util.concurrent.atomic.AtomicBoolean

class EndProcessSignalImpl(private val atomicBoolean : AtomicBoolean = AtomicBoolean()) : EndProcessSignal {
    override fun stopRunning() = atomicBoolean.get()
}