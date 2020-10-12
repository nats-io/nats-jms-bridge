# 0.26.0-beta23 NATS JMS/MQ Bridge

#### TAG: 0.26.0-beta23

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
