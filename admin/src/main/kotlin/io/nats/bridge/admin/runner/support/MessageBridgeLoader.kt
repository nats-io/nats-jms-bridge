package io.nats.bridge.admin.runner.support

import io.nats.bridge.admin.models.bridges.MessageBridgeInfo
import io.nats.bridge.support.MessageBridgeBuilder

data class BridgeConfig(val name:String, val builders:List<MessageBridgeBuilder>, val config: MessageBridgeInfo)

interface MessageBridgeLoader {
    fun loadBridgeConfigs(): List<BridgeConfig>
}