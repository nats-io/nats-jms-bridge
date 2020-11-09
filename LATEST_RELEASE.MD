# 0.29.0-beta26 NATS JMS/MQ Bridge

#### TAG: 0.29.0-beta26

## Issues


* #269 Dynamic bridge destination based on message header name



_____


#### Sample Bridge config file - nats-bridge.yaml.

```yaml
...
---
  bridges:
  - name: "natsToIBMMq"
    bridgeType: "DYNAMIC_FORWARD"
    transforms: [ "addHeader2" ]
    headerDestinationSubjectName: "DESTINATION_HEADER"
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
      ...
```

The bridge will look for a message header called `DESTINATION_HEADER`. The name of the
destination which can be a subject or JMS destination will be used to look up the JMS Queue or NATS subject.
The bridge will send to the subject/destination in the header, and it defaults to the subject mentioned in
the destination.
