# docker-bridge

### Entering Docker
```sh
docker exec -it bridge bash
```


### Testing
```sh
NATS_BRIDGE_JMS_USER=cloudurable NATS_BRIDGE_JMS_PWD=cloudurable NATS_BRIDGE_NATS_SERVERS=nats://nats-server:4222 NATS_BRIDGE_JMS_CONNECTION_FACTORY=tcp://active-mq:61616 ./gradlew clean test
```

###Check
