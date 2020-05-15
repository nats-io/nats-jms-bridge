package io.nats.bridge.admin.runner.support

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class HealthChecker  (private val messageBridgeRunner:MessageBridgeRunner): HealthIndicator {
    override fun health(): Health {
        return if (!messageBridgeRunner.isHealthy()) {
            Health.down().withDetail("NATS_MessageBridge", "Not Available - Was Started? ${messageBridgeRunner.wasStarted()} Is Running? ${messageBridgeRunner.isRunning()} ${messageBridgeRunner.getLastError()?.message} ${messageBridgeRunner.getLastError()?.javaClass?.simpleName}" ).build()
        } else Health.up().withDetail("NATS_MessageBridge", "Available").build()
    }
}