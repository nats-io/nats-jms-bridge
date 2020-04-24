package nats.io.nats.bridge.admin.runner

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

import javax.annotation.PreDestroy


@Component
class SpringBridgeRunner(private val bridgeRunner: BridgeRunner){

    @PostConstruct
    fun init() {
        bridgeRunner.initRunner()
    }

    @PreDestroy
    fun stop() {
        bridgeRunner.stopRunner()
    }

}
