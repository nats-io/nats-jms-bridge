# docker-bridge

#### Enter Docker
```sh
docker exec -it bridge bash
```


#### Running local dev environment with docker-deploy
```sh

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

#### Build NGINX container

```sh
pwd
~/synadia/nats-bridge/

docker build -t nginx-dev-natsbridge-admin -f cicd/Dockerfile_gradle_nginx.dev .
```

#### Run NGINX based admin container

```sh

docker run -p 443:443 nginx-dev-natsbridge-admin



```

#### To use admin command line with docker instance do the following

```sh

$ pwd
~synadia/nats-bridge/admin

$ export NATS_ADMIN_HOST=https://localhost:443

$ bin/admin.sh health                        
{
 "status": "UP"
}

```
