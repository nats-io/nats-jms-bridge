# 0.21.0-beta18 NATS JMS/MQ Bridge

#### TAG: 0.21.0-beta18

## Issues


* Make channel optional #216
* Github action #110 for release
* #178 initial benchmark data.
* #147 worked on this but not done.  
* #227 CCDT not getting read from JMS Context
* #228 document how to reproduce NATS TLS verify and test (in wiki too)
* #229 document how to test multi bridge setup for work share on multi bridge instances
* #230 document how to do IBM MQ failover test setup to verify IBM MQ failover config is working
* Added more sample configuration


---

## Initial perf testing

Issue #178.


* 9,467 request / response per second on a Macbook Pro running `nats-server` on the same machine.
* This equates to 37,869 messages per second. This is well above the requirements.
* Only the test harness was tuned.
* You can easily add 20 to 30 workers per server. This would be about 200K to 300K messages a second assuming the right hardware and network set up.




----


# TLS Keystore Alias issue #228

To fix this issue, we had to reproduce and to that we had to set up NATS to verify TLS.

To fix this issue we followed these:

## Removed the folder certs/keyalias/ with all content used in the previous version.

```sh
~/bridge2
cd certs/
rm -rf keyalias/
```

## Created a new folder called alias inside certs/

```sh
pwd
Output:
~/bridge2/certs
mkdir alias
```
Now create the **certificates**, **keystore** and **truststore**

### Certificates

Enter in the alias folder
```sh
cd alias

pwd
Output:
~/bridge2/certs/alias
```

## **Server Certificate**
```sh
mkcert -cert-file server-cert.pem -key-file server-key.pem localhost ::1

Output:
Using the local CA at "~/Library/Application Support/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "::1"

The certificate is at "server-cert.pem" and the key at "server-key.pem" ✅
```

## **Clients Certificates**

Client Certificate
```sh
mkcert -client -cert-file client-cert.pem -key-file client-key.pem localhost ::1

Output:
Using the local CA at "~/Library/Application Support/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "::1"

The certificate is at "client-cert.pem" and the key at "client-key.pem" ✅
```

## Create Sample Cloudurable Certificate

```sh
mkcert -client -cert-file cloudurable-cert.pem -key-file cloudurable-key.pem localhost ::1

Output:
Using the local CA at "~/Library/Application Support/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "::1"

The certificate is at "cloudurable-cert.pem" and the key at "cloudurable-key.pem" ✅
```

## Create sample Mamatus Certificate
```sh
mkcert -client -cert-file mamatus-cert.pem -key-file mamatus-key.pem localhost ::1

Outuput:
Using the local CA at "~/Library/Application Support/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "::1"

The certificate is at "mamatus-cert.pem" and the key at "mamatus-key.pem" ✅
```

Now we have 4 certificates and 4 certificates key, to check it list the folder content
```sh
ls

Output:
client-cert.pem		cloudurable-key.pem	server-cert.pem
client-key.pem		mamatus-cert.pem	server-key.pem
cloudurable-cert.pem	mamatus-key.pem
```

The tag -client for the client certificate tells to the truststore that this certificate will be used by a client.

This is needed to configure the tlsverify option on the Nats server.
When the flag tlsverify is set to true we need to configure the rootCA on the server.

## Get path for root CA
To obtain the path for the root CA type:
```sh
mkcert -CAROOT

Output:
~/Library/Application Support/mkcert


cp ~/Library/Application\ Support/mkcert/rootCA.pem rootCA.pem

ls

client-cert.pem		cloudurable-key.pem	rootCA.pem
client-key.pem		mamatus-cert.pem	server-cert.pem
cloudurable-cert.pem	mamatus-key.pem		server-key.pem
```

## Creating a P12 file

We need to create a p12 file to insert the file inside the keystore.  
You can hide the tag -password  and type the password when it's asked.  
If you prefer you can type your personal password after **pass:** parameter.

```sh
openssl pkcs12 -export -out client.p12 -inkey client-key.pem -in client-cert.pem -password pass:password -name "client-cert"

openssl pkcs12 -export -out cloudurable.p12 -inkey cloudurable-key.pem -in cloudurable-cert.pem -password pass:password -name "cloudurable-cert"

openssl pkcs12 -export -out mamatus.p12 -inkey mamatus-key.pem -in mamatus-cert.pem -password pass:password -name "mamatus-cert"
```
The parameter _**-name**_, is used to config the certificate alias.

Now you have 3 more files in the folder
```sh

$ ls

client-cert.pem		cloudurable-key.pem	mamatus.p12
client-key.pem		cloudurable.p12		rootCA.pem
client.p12		mamatus-cert.pem	server-cert.pem
cloudurable-cert.pem	mamatus-key.pem		server-key.pem
```
## Importing files to the keystore

```sh
keytool -importkeystore -srcstoretype PKCS12 -srckeystore client.p12 -srcstorepass password -destkeystore keystore.jks -deststorepass password -alias client-cert

keytool -importkeystore -srcstoretype PKCS12 -srckeystore cloudurable.p12 -srcstorepass password -destkeystore keystore.jks -deststorepass password -alias cloudurable-cert

keytool -importkeystore -srcstoretype PKCS12 -srckeystore mamatus.p12 -srcstorepass password -destkeystore keystore.jks -deststorepass password -alias mamatus-cert
```
**Note:** you need to use the same alias name set in the p12 file

The keystore file was created
```sh
$ ls

client-cert.pem		cloudurable.p12		rootCA.pem
client-key.pem		keystore.jks		server-cert.pem
client.p12		mamatus-cert.pem	server-key.pem
cloudurable-cert.pem	mamatus-key.pem
cloudurable-key.pem	mamatus.p12
```
## Creating the trustore file
```sh
keytool -importcert -trustcacerts -file rootCA.pem -storepass cloudurable2 -noprompt -keystore truststore.jks
```
Truststore was created
```sh
$ ls

client-cert.pem		cloudurable.p12		rootCA.pem
client-key.pem		keystore.jks		server-cert.pem
client.p12		mamatus-cert.pem	server-key.pem
cloudurable-cert.pem	mamatus-key.pem		truststore.jks
cloudurable-key.pem	mamatus.p12

```

## Now run NATS server with tlsverify.

```sh

nats-server -DV --tls --tlscert=~/job/nats-jms-mq-bridge2/bridge2/certs/alias/server-cert.pem /
--tlskey=~/job/nats-jms-mq-bridge2/bridge2/certs/alias/server-key.pem /
--tlscacert=~/job/nats-jms-mq-bridge2/bridge2/certs/alias/rootCA.pem --tlsverify=true

```

## Running the bridge

You can run the bridge and set the alias
```yml
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
          io.nats.client.tls.keystore.password: "password"
          io.nats.client.tls.truststore.password: "password"
          io.nats.client.tls.keystore.alias: "client-cert"
```

---

# 229 document how to test multi bridge setup for work share on multi bridge instances

#229

# Admin Bridge Share Work Walkthrough

### Creating a Docker Image with the latest bridge version

```yml
FROM ubuntu:20.04

#Copying files
COPY . ./usr/app

WORKDIR /usr/app

#Install some dependencies
RUN apt-get update
RUN apt-get -y install curl unzip zip wget nano jq

# Install SDK MAN
RUN chmod +x cicd/sdkman.sh
RUN exec cicd/sdkman.sh

# Install Java & Gradle
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install java 8.0.262-amzn"
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install gradle"

ENV JAVA_HOME /root/.sdkman/candidates/java/current

#Downloading the latest version
WORKDIR /usr/app/bridge
RUN bash -c "wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.19.0-beta15/nats-bridge-admin-0.19.0-beta15.zip"
RUN bash -c "unzip nats-bridge-admin-0.19.0-beta15.zip"
RUN bash -c "rm *.zip"

#Creating certs folder
RUN bash -c "mkdir certs"

#Copying certs
RUN bash -c "cp -r /usr/app/certs/* certs/"
```

### Building and uploading docker image
```sh
$ pwd
Output:
~/nats-jms-mq-bridge2/bridge2

$ docker build -f cicd/gradle_version/Dockerfile -t cloudurable/multi-bridge .

Final Output:
Successfully tagged cloudurable/multi-bridge:latest

$ docker push cloudurable/multi-bridge
Output:
latest: digest: sha256:ee5cdc79a72de5ed9721628773c59bd2804ae4ad4dea05dd04eb7f46ae7460db size: 3884
```
### Creating a docker compose file
```yml
# Test that uses docker image of IBM MQ, Nats, active mq and Multbridge instances.
# Use this docker compose file to test bridge share work.
# We change the bridge port forward because we will run admin in the localhost too.
# You can create as many bridge admin as you want/need
version: '3'
services:
  nats-server:
    build:
      context: ../bridge-nats-server
    ports:
      - "4222:4222"
      - "8222:8222"
  ibm-mq:
    build:
      context: ../bridge-ibmmq
    environment:
      - LICENSE=accept
      - MQ_QMGR_NAME=QM1
      - MQ_APP_PASSWORD=passw0rd
    ports:
      - "1414:1414"
      - "8443:9443"
  active-mq:
    build:
      context: ../bridge-activemq
    environment:
        - ARTEMIS_USERNAME=cloudurable
        - ARTEMIS_PASSWORD=cloudurable
    ports:
      - "8161:8161"
      - "61616:61616"
  bridge-admin_1:
    container_name: bridge-admin_1
    image: 'cloudurable/multi-bridge'
    environment:
      - TARGET=host.docker.internal
    working_dir: /usr/app/bridge
    command: ["sleep","infinity"]
    ports:
      - "8070:8080"
  bridge-admin_2:
    container_name: bridge-admin_2
    image: 'cloudurable/multi-bridge'
    environment:
      - TARGET=host.docker.internal
    working_dir: /usr/app/bridge
    command: ["sleep","infinity"]
    ports:
      - "8090:8080"
  bridge-admin_3:
    container_name: bridge-admin_3
    image: 'cloudurable/multi-bridge'
    environment:
      - TARGET=host.docker.internal
    working_dir: /usr/app/bridge
    command: ["sleep","infinity"]
    ports:
      - "8060:8080"
```

### Install guide for NATS JMS/MQ Bridge

### Download the distribution zip and unzip it
```sh
mkdir bridge
cd bridge

wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.19.0-beta15/nats-bridge-admin-0.19.0-beta15.zip
unzip nats-bridge-admin-0.19.0-beta15.zip
rm *.zip

```
### Before you run the server bridge
Before you run the server you may want to download the source code and run the docker-compose out of
the cicd folder which starts up IBM MQ, ActiveMQ, NATS Servers and 3 Bridges in Docker.

```sh
git clone https://github.com/nats-io/nats-jms-mq-bridge.git
cd nats-jms-mq-bridge
bin/build.sh multbridge
```
The command bin/build.sh multbridge uses docker-deploy to deploy IBM MQ, ActiveMQ, NATS Servers and 3 Bridges for testing and development.

Use the follow command to see dockers running.

```sh
$ docker ps

Output:
CONTAINER ID        IMAGE                      COMMAND                  CREATED             STATUS              PORTS                                                                                                 NAMES
6b7e7d1544a2        compose_nats-server        "docker-entrypoint.s…"   3 minutes ago       Up 3 minutes        0.0.0.0:4222->4222/tcp, 0.0.0.0:8222->8222/tcp, 6222/tcp                                              compose_nats-server_1
07a8b085d1c1        cloudurable/multi-bridge   "sleep infinity"         3 minutes ago       Up 3 minutes        0.0.0.0:8090->8080/tcp                                                                                bridge-admin_2
8a29b378b235        compose_ibm-mq             "runmqdevserver"         3 minutes ago       Up 3 minutes        0.0.0.0:1414->1414/tcp, 9157/tcp, 0.0.0.0:8443->9443/tcp                                              compose_ibm-mq_1
3c923450dc7f        cloudurable/multi-bridge   "sleep infinity"         3 minutes ago       Up 3 minutes        0.0.0.0:8070->8080/tcp                                                                                bridge-admin_1
ac430609e5f5        compose_active-mq          "/docker-entrypoint.…"   3 minutes ago       Up 3 minutes        1883/tcp, 5445/tcp, 5672/tcp, 9404/tcp, 0.0.0.0:8161->8161/tcp, 61613/tcp, 0.0.0.0:61616->61616/tcp   compose_active-mq_1
60bfcdcbf735        cloudurable/multi-bridge   "sleep infinity"         3 minutes ago       Up 3 minutes        0.0.0.0:8060->8080/tcp                                                                                bridge-admin_3
```
You have 3 bridge-admins running: **bridge-admin_1, bridge-admin_2, bridge-admin_3**


### Use the sample JSSL certs
This copies the sample cert files so that the sample bridge config can find the JSSL certs.

```sh
$ pwd
~/bridge

$ mkdir certs                       

$ cp nats-jms-mq-bridge/certs/* certs/
```

# Run the application
```sh
cd ..
pwd
~/bridge

cd nats-bridge-admin-*
````

### Run the server
```sh
bin/nats-bridge-admin
Output

 .   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
 '  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::        (v2.2.6.RELEASE)

2020-05-01 03:22:06.114  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Starting ApplicationMain on Richards-MacBook-Pro.local with PID 92828 (/Users/richardhightower/bridge/nats-bridge-admin-0.11.1-beta7/lib/nats-bridge-admin-0.11.1-beta7.jar started by richardhightower in /Users/richardhightower/bridge/nats-bridge-admin-0.11.1-beta7)
2
...
2020-05-01 03:22:09.211  INFO 92828 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2020-05-01 03:22:09.214  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Started ApplicationMain in 3.409 seconds (JVM running for 3.688)
```
Note that after you run the bridge for the first time the following files should be present in the config folder.
```sh
ls config/*

config/initial-nats-bridge-logins.json	config/logback.xml			config/nats-bridge.yaml
config/initial-nats-bridge-logins.yaml	config/nats-bridge-logins.yaml
```

### Using the command line tools
To use this tool you must install jq.
jq is a lightweight command-line JSON processor.
https://stedolan.github.io/jq/
(brew install jq or sudo apt-get install jq or https://stedolan.github.io/jq/download/)

**If you did not run the integration test then you need to generate the token file before you use the admin.**

To set up **admin tool** for the first time from the NATS Bridge Admin directory run set-up-admin
```sh
  $ bin/admin.sh set-up-admin
```

This will create the admin token under config/admin.token. This token is a JWT token that gets used
by the admin. Once you generate the admin.token, you may want to delete the config/initial-nats-bridge-logins.yaml and config/initial-nats-bridge-logins.json files.

See that the token file exists:
```sh
ls config/*
config/admin.token			config/initial-nats-bridge-logins.yaml	config/nats-bridge-logins.yaml
config/initial-nats-bridge-logins.json	config/logback.xml			config/nats-bridge.yaml
```
Note the config/admin.token file that was generated from set-up-admin.

### Copying the **nats-bridge.yaml** file to run this test
```sh
rm config/nats-bridge.yaml
cp sampleConf/nats-bridge-local.yaml config/nats-bridge.yaml
````
Run the server again and check if you don't have errors
```sh
bin/nats-bridge-admin
```
### Running mult-bridges
To run mult-bridges we need to change the nats-bridge.yaml inside the dockers.
To do that follow the steps:

Open a new terminal and list the dockers
```sh
$ docker ps

Output:

CONTAINER ID        IMAGE                      COMMAND                  CREATED             STATUS              PORTS                                                                                                 NAMES
...
3c923450dc7f        cloudurable/multi-bridge   "sleep infinity"         40 minutes ago      Up 40 minutes       0.0.0.0:8070->8080/tcp                                                                                bridge-admin_1
...
```

I will connect to the first bridge admin using the CONTAINER ID

```sh
docker exec -it 3c923450dc7f bash
Output
root@3c923450dc7f:/usr/app/bridge#
```

Enter in the nats-admin folder and copy the config files
```sh
cd nats-bridge-admin-*

cp /usr/app/admin/config/* config/
```
Run the admin inside the docker
```sh
bin/nats-bridge-admin
```

**Repeat this steps to the others admin dockers**

### Running the integration test to see the work share

Open another terminal and go to the bridge folder
```sh
cd ~/bridge/nats-bridge-admin-*

$ bin/integration.sh
```

You will see the bridges sharing work

____

# IBM MQ Failover test (230)

#230


### Creating a docker compose file
```yml
# Test that uses docker image of mult IBM MQ, Nats and active mq.
# Use this docker compose file to test IBM MQ failover.
# We change the IBM MQ port forward to run many docker instances at the same time
version: '3'
services:
  nats-server:
    build:
      context: ../bridge-nats-server-tls
    ports:
      - "4222:4222"
      - "8222:8222"
  active-mq:
    build:
      context: ../bridge-activemq
    environment:
        - ARTEMIS_USERNAME=cloudurable
        - ARTEMIS_PASSWORD=cloudurable
    ports:
      - "8161:8161"
      - "61616:61616"
  ibm-mq_1:
    build:
      context: ../bridge-ibmmq
    environment:
      - LICENSE=accept
      - MQ_QMGR_NAME=QM1
      - MQ_APP_PASSWORD=passw0rd
    ports:
      - "1414:1414"
  ibm-mq_2:
    build:
      context: ../bridge-ibmmq
    environment:
      - LICENSE=accept
      - MQ_QMGR_NAME=QM1
      - MQ_APP_PASSWORD=passw0rd
    ports:
      - "1415:1414"
  ibm-mq_3:
    build:
      context: ../bridge-ibmmq
    environment:
      - LICENSE=accept
      - MQ_QMGR_NAME=QM1
      - MQ_APP_PASSWORD=passw0rd
    ports:
      - "1416:1414"
```

### Install guide for NATS JMS/MQ Bridge

### Download the distribution zip and unzip it
```sh
mkdir bridge
cd bridge

wget https://github.com/nats-io/nats-jms-mq-bridge/releases/download/0.19.0-beta15/nats-bridge-admin-0.19.0-beta15.zip
unzip nats-bridge-admin-0.19.0-beta15.zip
rm *.zip

```
### Before you run the server bridge
Before you run the server you may want to download the source code and run the docker-compose out of
the cicd folder which starts up IBM MQ, ActiveMQ, NATS Servers and 3 Bridges in Docker.

```sh
git clone https://github.com/nats-io/nats-jms-mq-bridge.git
cd nats-jms-mq-bridge
bin/build.sh multibm
```
The command bin/build.sh multibm uses docker-deploy to deploy Mult IBM MQ, ActiveMQ and NATS Servers for testing and development.

Use the follow command to see dockers running.

```sh
$ docker ps

Output:
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                                                                 NAMES
636685f372cd        compose_ibm-mq_1      "runmqdevserver"         19 seconds ago      Up 18 seconds       9157/tcp, 0.0.0.0:1414->1414/tcp, 9443/tcp                                                            compose_ibm-mq_1_1
2cb2cc19aaeb        compose_ibm-mq_2      "runmqdevserver"         19 seconds ago      Up 18 seconds       9157/tcp, 9443/tcp, 0.0.0.0:1415->1414/tcp                                                            compose_ibm-mq_2_1
55eb6c29d1ba        compose_active-mq     "/docker-entrypoint.…"   19 seconds ago      Up 18 seconds       1883/tcp, 5445/tcp, 5672/tcp, 9404/tcp, 0.0.0.0:8161->8161/tcp, 61613/tcp, 0.0.0.0:61616->61616/tcp   compose_active-mq_1
4bb3344b9746        compose_ibm-mq_3      "runmqdevserver"         19 seconds ago      Up 18 seconds       9157/tcp, 9443/tcp, 0.0.0.0:1416->1414/tcp                                                            compose_ibm-mq_3_1
c47c68d248a1        compose_nats-server   "docker-entrypoint.s…"   19 seconds ago      Up 18 seconds       0.0.0.0:4222->4222/tcp, 0.0.0.0:8222->8222/tcp, 6222/tcp                                              compose_nats-server_1
```
You have 3 IBM-MQ running: **compose_ibm-mq_1_1, compose_ibm-mq_2_1, compose_ibm-mq_3_1**

# Run the application
```sh
cd ..
pwd
~/bridge

cd nats-bridge-admin-*
````

### Run the server
```sh
bin/nats-bridge-admin
Output

 .   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
 '  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::        (v2.2.6.RELEASE)

2020-05-01 03:22:06.114  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Starting ApplicationMain on Richards-MacBook-Pro.local with PID 92828 (/Users/richardhightower/bridge/nats-bridge-admin-0.11.1-beta7/lib/nats-bridge-admin-0.11.1-beta7.jar started by richardhightower in /Users/richardhightower/bridge/nats-bridge-admin-0.11.1-beta7)
2
...
2020-05-01 03:22:09.211  INFO 92828 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2020-05-01 03:22:09.214  INFO 92828 --- [           main] io.nats.bridge.admin.ApplicationMain     : Started ApplicationMain in 3.409 seconds (JVM running for 3.688)
```
Note that after you run the bridge for the first time the following files should be present in the config folder.
```sh
ls config/*

config/initial-nats-bridge-logins.json	config/logback.xml			config/nats-bridge.yaml
config/initial-nats-bridge-logins.yaml	config/nats-bridge-logins.yaml
```

### Using the command line tools
To use this tool you must install jq.
jq is a lightweight command-line JSON processor.
https://stedolan.github.io/jq/
(brew install jq or sudo apt-get install jq or https://stedolan.github.io/jq/download/)

**If you did not run the integration test then you need to generate the token file before you use the admin.**

To set up **admin tool** for the first time from the NATS Bridge Admin directory run set-up-admin
```sh
  $ bin/admin.sh set-up-admin
```

This will create the admin token under config/admin.token. This token is a JWT token that gets used
by the admin. Once you generate the admin.token, you may want to delete the config/initial-nats-bridge-logins.yaml and config/initial-nats-bridge-logins.json files.

See that the token file exists:
```sh
ls config/*
config/admin.token			config/initial-nats-bridge-logins.yaml	config/nats-bridge-logins.yaml
config/initial-nats-bridge-logins.json	config/logback.xml			config/nats-bridge.yaml
```
Note the config/admin.token file that was generated from set-up-admin.

### Copying the **nats-bridge.yaml** file to run this test
```sh
rm config/nats-bridge.yaml
cp sampleConf/ibm_mq_failover_request_reply.yaml config/nats-bridge.yaml
````
Run the server again and check if you don't have errors
```sh
bin/nats-bridge-admin
```
### Running the test and kill ibm mq to see failover

Open another terminal and prepare to kill the first IBM MQ docker
```sh
docker ps

CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                                                                 NAMES
340e3327625b        compose_ibm-mq_1      "runmqdevserver"         33 minutes ago      Up 33 minutes       9157/tcp, 0.0.0.0:1414->1414/tcp, 9443/tcp                                                            compose_ibm-mq_1_1

```
Prepare the command to kill the docker

```sh
docker kill 340e3327625b
```

Open another terminal and go to the bridge folder
```sh
cd ~/bridge/nats-bridge-admin-*

$ bin/integration.sh
```
Kill the docker in the other terminal, you will see that the job still running  

If you want, repeat this step and kill the second IBM MQ docker.  
