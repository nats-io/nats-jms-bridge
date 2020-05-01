package io.nats.bridge.admin.integration

object Constants {
    const val host = "http://localhost:8080"
    const val bridgeControlURL = "$host/api/v1/control/bridges"
    const val bridgeControlAdminURL = "$host/api/v1/control/bridges/admin"
    const val loginURL = "$host/api/v1/login"
    const val initialYaml = "config/initial-nats-bridge-logins.yaml"
    const val adminToken = "config/admin.token"
    const val natsBridgeConfigFileName = "./config/nats-bridge.yaml"

}