# 0.16.0-beta11 NATS JMS/MQ Bridge

#### TAG: 0.16.0-beta11

## Issues

* #196 MTLS support via System properties
* #189 and #198 Subscription support to support multiple bridge admins   
* Option to select private key from key store using alias #172

----

#### Sample nats-bridge.yaml Showing passing system properties

```yaml

---
name: "Sample Showing passig system properties"
systemProperties:
  java.net.ssl.trustStore: path to trust store
  javax.net.ssl.keyStore: path to key store
  javax.net.ssl.keyStorePassword: sslpass

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
```
