# 0.16.0-beta11 NATS JMS/MQ Bridge

#### TAG: 0.16.0-beta11

## Issues

* #196 MTLS support via System properties
* #189 and #198 Subscription support to support multiple bridge admins   
* Option to select private key from key store using alias #172

----

#### Sample nats-bridge.yaml Showing passing system properties, keystore alias, and MTLS

```yaml

---
name: "Sample Showing passig system properties"
systemProperties:
  javax.net.ssl.trustStore: "../certs/ibmkey.jks" # <-- MTLS #196
  javax.net.ssl.trustStoreType: JKS
  com.ibm.mq.cfg.useIBMCipherMappings: false # <-- MTLS #196
  javax.net.ssl.trustStorePassword: cloudurable@10
  # javax.net.ssl.keyStore: path to key store
  # javax.net.ssl.keyStorePassword: sslpass  

bridges:
  - name: "natsToIBMMq"
    bridgeType: "REQUEST_REPLY"
    transforms: ["addHeader"]
    replyTransforms : ["changeBody"]
    source:
      name: "nats"
      busType: "NATS"
      subject: "b-subject"
      clusterName: "natsCluster"
    destination:
      name: "ibmMQ"
      busType: "JMS"
      subject: "DEV.QUEUE.1"
      clusterName: "ibmMqCluster"
    copyHeaders: false
    workers: 5
    tasks : 2

...

ibmMqCluster:
  name: "ibmMqCluster"
  properties: !<jms>
    config:
      java.naming.factory.initial: "io.nats.bridge.integration.ibmmq.IbmMqInitialContextFactory"
      nats.ibm.mq.host: "tcp://localhost:1414"
      nats.ibm.mq.channel: "DEV.APP.SVRCONN"
      nats.ibm.mq.queueManager: "QM1"
      nats.ibm.mq.queueModelName: "DEV.MODEL"
      nats.ibm.mq.queueModelPrefix: "DEV*"
      io.nats.ibm.mq.jms.prop.XMSC_WMQ_SSL_CIPHER_SUITE: “TLS_RSA_WITH_AES_128_CBC_SHA256” # <-- MTLS #196
    userName: "app"
    password: "passw0rd"
    jmsDestinationType: "QUEUE"    

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
          io.nats.client.tls.truststore.path: "../certs/truststore.crt"
          io.nats.client.tls.keystore.path: "../certs/keystore.jks"
          io.nats.client.tls.algorithm: "SunX509"
          io.nats.client.tls.keystore.password: "cloudurable1"
          io.nats.client.tls.truststore.password: "cloudurable2"
          keystore.alias.keystore.alias: "myalias"   ## <------------- #172 keys tore alias

```
