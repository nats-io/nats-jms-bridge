# Repackage NATS JMS Bridge Spring Boot Admin Application to create a custom Spring Executable Jar to run the NATS JMS Bridge

To extend the spring boot application, you need the following dependencies. 
	
#### pom.xml 

```xml
                <dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge-message</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>

		<dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>

		<dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge-springboot-app</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>
```

* ***nats-jms-bridge-message*** - holds transform and message builders, can be used in your client code as well and is needed for custom transformations
* ***nats-jms-bridge*** - the core library which implements the bridge code to bridge between NATS to/fro JMS (ActiveMQ and IBM MQ). 
* ***nats-jms-bridge-springboot-app*** - the spring boot admin application which implements health checkpoints, integrates with Prometheus, allows administration of bridges, allows config via YAML, implements JWT, allows imports of csv files for IBM MQ, and provides a REST interface for controlling the NATS Bridge so it can be administered 

You can find the bridge dependencies in the [Maven Central Repository](https://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22io.nats.bridge%22) which is available and searchable with sample gradle, maven, and sbt dependency declaration from [this maven repo search web tool](https://mvnrepository.com/artifact/io.nats.bridge). 

You can find the source code for this example in [the main github repo for this project, look for mavenBridge](https://github.com/nats-io/nats-jms-mq-bridge/tree/master/mavenBridge).

To extend the NATS JMS Bridge Admin just create a SpringApplication class as follows:

#### src/main/java/com/example/mavenBridge/MavenBridgeApplication.java - MavenBridgeApplication
```java

package com.example.mavenBridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MavenBridgeApplication {

	public static void main(String[] args) {
		String [] actualArgs = args.length == 0 ? new String[]{"--config-directory", "./BOOT-INF/classes/config/"} : args;
		io.nats.bridge.admin.ApplicationMain.main(actualArgs);
	}

}

``` 

Notice that we can pass any of the command line arguments to the ApplicationMain of the NATS bridge admin. 
You can also set environments variables by replacing dashes '-' with underscores '_' and prefix with "NATS_BRIDGE" 
    
```bash
    NATS_BRIDGE_LOGIN_CONFIG_FILE=./config/nats-bridge-logins.yaml
    NATS_BRIDGE_BRIDGE_CONFIG_FILE=./config/nats-bridge.yaml
```
    
    Files can also be on the classpath inside of a jar file or on the file system in the classpath. 
    Just prepend the file name with "classpath://" to denote looking for this file on the classpath instead of the file system.
    
```bash
    -f classpath://nats-bridge.yaml
```

You can specify these command-line arguments.

```bash
    configFolder use "-d" or "--config-directory", This is the Location of Configuration Directory and defaults to ("./config/").
    bridgeConfigFile use "-f" or "--bridge-config-file", This is the Location of Bridge Config File.
    loginConfigFile use "-l" or "--login-config-file", This is the Location of Bridge Login Config File.
```

In the above example, we pass the location of the `"--config-directory"`. 
This allows you to bundle special config. 

To create an executable spring boot jar file, we use the following maven pom file. 

#### pom.xml - Maven build file. 

```xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.3.3.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.example</groupId>
	<artifactId>mavenBridge</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>mavenBridge</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>

		<dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge-message</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>

		<dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>

		<dependency>
			<groupId>io.nats.bridge</groupId>
			<artifactId>nats-jms-bridge-springboot-app</artifactId>
			<version>0.21.3-beta18</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.module</groupId>
			<artifactId>jackson-module-parameter-names</artifactId>
			<version>2.11.2</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.datatype</groupId>
			<artifactId>jackson-datatype-jdk8</artifactId>
			<version>2.9.5</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.datatype</groupId>
			<artifactId>jackson-datatype-jsr310</artifactId>
			<version>2.11.2</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.module</groupId>
			<artifactId>jackson-module-kotlin</artifactId>
			<version>2.9.5</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.dataformat</groupId>
			<artifactId>jackson-dataformat-yaml</artifactId>
			<version>2.9.8</version>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
			<exclusions>
				<exclusion>
					<groupId>org.junit.vintage</groupId>
					<artifactId>junit-vintage-engine</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>

```

## Building with maven

To build this project use the command `mvn package` as follows:

```bash 
./mvnw package

...
[INFO] --- spring-boot-maven-plugin:2.3.3.RELEASE:repackage (repackage) @ mavenBridge ---
[INFO] Replacing main artifact with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.966 s
[INFO] Finished at: 2020-09-17T18:57:23-07:00
[INFO] ------------------------------------------------------------------------

```

## Run the application 

To run the application do this after running the above build:

Find the jar. 

```bash
$ find . -name "*Bridge*.jar"


./target/mavenBridge-0.0.1-SNAPSHOT.jar
```  

Run the jar with `java -jar`. 

```bash 
java -jar ./target/mavenBridge-0.0.1-SNAPSHOT.jar


  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.3.RELEASE)

2020-09-17 18:59:26.238  INFO 7312 --- [           main] c.github.ajalt.clikt.core.CliktCommand   : Starting CliktCommand on Richards-MacBook-Pro.local with PID 7312 (/Users/richardhightower/synadia/nats-jms-mq-bridge/mavenBridge/target/mavenBridge-0.0.1-SNAPSHOT.jar started by richardhightower in /Users/richardhightower/synadia/nats-jms-mq-bridge/mavenBridge)

...

2020-09-17 18:59:32.571  INFO 7312 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-09-17 18:59:32.733  INFO 7312 --- [           main] o.a.coyote.http11.Http11NioProtocol      : Starting ProtocolHandler ["http-nio-8080"]
2020-09-17 18:59:32.752  INFO 7312 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2020-09-17 18:59:32.753  INFO 7312 --- [           main] d.s.w.p.DocumentationPluginsBootstrapper : Context refreshed
2020-09-17 18:59:32.775  INFO 7312 --- [           main] d.s.w.p.DocumentationPluginsBootstrapper : Found 1 custom documentation plugin(s)
2020-09-17 18:59:32.833  INFO 7312 --- [           main] s.d.s.w.s.ApiListingReferenceScanner     : Scanning for api listing references
2020-09-17 18:59:33.036  INFO 7312 --- [           main] c.github.ajalt.clikt.core.CliktCommand   : Started CliktCommand in 7.455 seconds (JVM running for 8.198)

```
 
The above ran because the config in the directory `./BOOT-INF/classes/config/` which was specified with `MavenBridgeApplication` contains yaml config compatible with our CI/CD docker-compose file used to integration test the NATS JMS Bridge. 


If you want to test this out fully, do the following.

```
git clone https://github.com/nats-io/nats-jms-mq-bridge.git
cd nats-jms-mq-bridge

## Run docker-deploy for integration testing NATS JMS Bridge  
bin/build.sh localdev


```

```
nats-server_1  | [1] 2020/09/18 00:26:38.851264 [INF] Starting nats-server version 2.1.6
nats-server_1  | [1] 2020/09/18 00:26:38.851387 [INF] Git commit [8c8d6f8]
nats-server_1  | [1] 2020/09/18 00:26:38.851797 [INF] Listening for client connections on 0.0.0.0:4222
nats-server_1  | [1] 2020/09/18 00:26:38.851832 [INF] TLS required for client connections
nats-server_1  | [1] 2020/09/18 00:26:38.851844 [INF] Server id is NDR5GY6NN253LXYNB7TSOWDTDDVRY3DQXU2P2MSLG5YUVJVCLX2AAFUP
nats-server_1  | [1] 2020/09/18 00:26:38.851912 [INF] Server is ready
ibm-mq_1       | 2020-09-18T00:26:38.899Z CPU architecture: amd64
...
ibm-mq_1       | 2020-09-18T00:26:42.047Z AMQ8024I: IBM MQ channel initiator started.
active-mq_1    | 48000
active-mq_1    |      _        _               _
active-mq_1    |     / \  ____| |_  ___ __  __(_) _____
active-mq_1    |    / _ \|  _ \ __|/ _ \  \/  | |/  __/
active-mq_1    |   / ___ \ | \/ |_/  __/ |\/| | |\___ \
active-mq_1    |  /_/   \_\|   \__\____|_|  |_|_|/___ /
active-mq_1    |  Apache ActiveMQ Artemis 2.12.0
...
```

Once the JMS and NATS servers are running, you can restart the NATS Bridge admin and then run the integration test. 

Now run runIntegrationNatsToMQ tasks from the admin folder to run the integration tests. 

```bash

$ ./gradlew runIntegrationNatsToMQ


## Output 
...
Call 98 of run 19
Call 99 of run 19
############### REPLY COUNT 2000 of 2000 in time 11434
Running? true
Started? true
Errors? false
TOTAL SENT ############### 2000 in time 1441
```

The task runIntegrationNatsToMQ just runs this gradle task which runs the class `IntegrationRequestReplyMain`. 
 
#### build.gradle.kts - runIntegrationNatsToMQ
```kotlin 
    create<JavaExec>("runIntegrationNatsToMQ") {
        main = "io.nats.bridge.admin.integration.IntegrationRequestReplyMain"
        classpath = sourceSets["main"].runtimeClasspath
    }

```

If `runIntegrationNatsToMQ` is missing just add it. It will be in the next release. 


 


