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


## This gets used by NATS Server Docker image and NGINX Mkcert installation latest version

## To see the latest version go to https://github.com/FiloSottile/mkcert/releases


```
apt install libnss3-tools wget -y
```

```
mkdir certs

cd certs

# wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.1/mkcert-v1.4.1-linux-amd64
# mv mkcert-v1.4.1-linux-amd64 mkcert
# chmod +x mkcert
# cp mkcert /usr/local/bin/

# mkcert -install

# mkcert -CAROOT (see CA path)

# mkcert localhost

```

Then use this site to generate the config.

## The SSL configuration generator tool for NGINX

https://ssl-config.mozilla.org/#server=apache&version=2.4.41&config=intermediate&openssl=1.1.1d&guideline=5.4
