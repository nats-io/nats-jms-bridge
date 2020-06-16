# 0.10.0-beta6 NATS JMS/MQ Bridge

#### TAG: 0.10.0-beta6

## Issues

* #148 Break Message and transformation API into its own jar file.
* #155 Fix truststore/keystore passcode issue
* #152 Add read from the Base64 keystore environment variable feature.
___

## Using ENV variables and command line args to use classpath://
```
Usage: run [OPTIONS]

  Run NATS JMS/IBM MQ Bridge

Options:
  -d, --config-directory TEXT    Location of Configuration Directory
  -f, --bridge-config-file TEXT  Location of Bridge Config File
  -l, --login-config-file TEXT   Location of Bridge Login Config File
  -h, --help                     Show this message and exit

You can also set environments variables by replacing dashes '-' with
underscores '_' and prefix with "NATS_BRIDGE"

NATS_BRIDGE_LOGIN_CONFIG_FILE=./config/nats-bridge-logins.yaml
NATS_BRIDGE_BRIDGE_CONFIG_FILE=./config/nats-bridge.yaml

Files can also be on the classpath inside of a jar file or on the file system
in the classpath. Just prepend the file name with "classpath://" to denote
looking for this file on the classpath instead of the file system.

-f classpath://nats-bridge.yaml
```

____

#### NATS Bridge config so a keystore can be read from env variables 
```yaml
natsCluster:
  name: "natsCluster"
  properties: !<nats>
    host: "localhost"
    port: 4222
    servers: []
    config:
      io.nats.client.reconnect.wait: 3000
      io.nats.client.reconnect.max: 10
      io.nats.client.timeout: 4000
      io.nats.client.tls.jssl.enable: true
      #io.nats.client.tls.truststore.path: ../certs/truststore.jks
      #io.nats.client.tls.keystore.path: ../certs/keystore.jks
      io.nats.client.tls.truststore.env_var.path: TRUSTSTORE
      io.nats.client.tls.keystore.env_var.value: KEYSTORE
      io.nats.client.tls.algorithm: SunX509
      io.nats.client.tls.store.password: cloudurable
      io.nats.client.tls.trust.password: cloudurable

```
