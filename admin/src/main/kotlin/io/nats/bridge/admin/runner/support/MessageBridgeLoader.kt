package io.nats.bridge.admin.runner.support

interface MessageBridgeLoader {
    fun loadBridgeBuilders(): List<MessageBridgeBuilder>

    fun loadBridges() = loadBridgeBuilders().map { it.build() }
}