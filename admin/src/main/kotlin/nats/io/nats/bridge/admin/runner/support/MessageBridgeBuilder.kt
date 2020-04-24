package nats.io.nats.bridge.admin.runner.support

import io.nats.bridge.support.MessageBusBuilder


class MessageBridgeBuilder {

    var sourceBusBuilder: MessageBusBuilder? = null
    var destBusBuilder: MessageBusBuilder? = null


    fun withSourceBusBuilder(sourceBusBuilder: MessageBusBuilder) = apply { this.sourceBusBuilder = sourceBusBuilder }
    fun withDestinationBusBuilder(sourceBusBuilder: MessageBusBuilder) = apply { this.sourceBusBuilder = sourceBusBuilder }

}