# 0.27.0-beta24 NATS JMS/MQ Bridge

#### TAG: 0.27.0-beta24

## Issues

* #271 Base64 encoding for truststore, keystore stored on env vars
* Patch for #271 apply to NATS #275

_____


NATS bridge will look for key store password and truststore password under the environment variables

```
"NATS_BRIDGE_KEY_PASS_ENV"  -> "Y2xvdWR1cmFibGUx",
"NATS_BRIDGE_TRUST_PASS_ENV" ->  "Y2xvdWR1cmFibGUy"
```

Example

```kotlin

create<JavaExec>("runIntegrationNatsToMQ") {
    main = "io.nats.bridge.admin.integration.IntegrationRequestReplyMain"
    classpath = sourceSets["main"].runtimeClasspath
    environment(mapOf(
        "NATS_BRIDGE_KEY_PASS_ENV" to "Y2xvdWR1cmFibGUx",
        "NATS_BRIDGE_TRUST_PASS_ENV" to "Y2xvdWR1cmFibGUy"
    ))
}

```

You can now specify which env var to use in the bridge config file under the NATs cluster section. 

```yaml
...
natsCluster:
  name: "natsCluster"
  properties: !<nats>
    host: "localhost"
    port: 4222
    servers: []
    config:
      io.nats.client.reconnect.wait: "3000"
      io.nats.client.reconnect.max: "10"
      io.nats.client.timeout: "4000"
      io.nats.client.tls.jssl.enable: "true"
      io.nats.client.tls.truststore.path: "../certs/truststore.jks"
      io.nats.client.tls.keystore.path: "../certs/keystore.jks"
      io.nats.client.tls.algorithm: "SunX509"
      io.nats.client.tls.keystore.env_var.base64password: "NATS_BRIDGE_KEY_PASS_ENV"
      io.nats.client.tls.truststore.env_var.base64password: "NATS_BRIDGE_TRUST_PASS_ENV"
      # NATS_BRIDGE_KEY_PASS_ENV=Y2xvdWR1cmFibGUx;NATS_BRIDGE_TRUST_PASS_ENV=Y2xvdWR1cmFibGUy
```
