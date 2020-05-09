# docker-bridge

#### Enter Docker
```sh
docker exec -it bridge bash
```


#### Running local dev environment
```
pwd
synadia/nats-bridge/cicd

docker-compose -f docker-compose-demo.yml up

### OR
synadia/nats-bridge

bin/docker-deploy-local-dev.sh

```


#### Testing
```sh

pwd
synadia/nats-bridge/core

./gradlew clean test
```
