package io.nats.bridge.admin.runner.support

interface EndProcessSignal {
    fun stopRunning(): Boolean
    fun keepRunning() = !stopRunning()
}