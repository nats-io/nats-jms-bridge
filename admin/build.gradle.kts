import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {

    repositories {
        maven("https://repo1.maven.org/maven2")
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.2.6.RELEASE")
    }
}

plugins {

    val kotlinVersion = "1.3.71"
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("maven-publish")
    application
    distribution
    id("org.springframework.boot") version "2.2.6.RELEASE"
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("signing")
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenLocal()
    mavenCentral()
}

val jarVersion = "1.0.0"
val isRelease = System.getenv("BUILD_EVENT") == "release"

// version is the variable the build actually uses.
version = if (isRelease) jarVersion else jarVersion + "-SNAPSHOT"
val libType = if (isRelease) "" else "-SNAPSHOT"

//tasks.named<Javadoc>("javadoc") {
//    (options as StandardJavadocDocletOptions).addStringOption("jaxrscontext", "http://localhost:8080/myapp")
//    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
//}

java {
    withSourcesJar()
    withJavadocJar()
}

springBoot {
    mainClassName = "io.nats.bridge.admin.ApplicationMain"
    buildInfo {
        properties {
            additional = mapOf(
                    "release" to "1.0.0",
                    "author" to "NATS team"
            )
        }
    }
}

application {
    mainClassName = "io.nats.bridge.admin.ApplicationMain"
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
    packageGroup.set("io.nats.bridge")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "io.nats.bridge"
            artifactId = "nats-jms-bridge-springboot-app"

            from(components["java"])
            pom {
                name.set(rootProject.name)
                packaging = "jar"
                groupId = "io.nats.bridge"
                artifactId = "nats-jms-bridge-springboot-app"
                description.set("NATS.IO Java JMS Bridge Admin App")
                url.set("https://github.com/nats-io/nats-jms-bridge")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("synadia")
                        name.set("Synadia")
                        email.set("info@synadia.com")
                        url.set("https://nats.io")
                    }
                }
                scm {
                    url.set("https://github.com/nats-io/nats-jms-bridge")
                }
            }
        }
    }
}

if (isRelease) {
    signing {
        val signingKeyId = System.getenv("SIGNING_KEY_ID")
        val signingKey = System.getenv("SIGNING_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(configurations.archives.get())
        sign(publishing.publications["mavenJava"])
    }
}

distributions {
    main {
        contents {
            from("bin") {
                into("bin")
            }
            from("sampleConf/") {
                into("sampleConf/")
            }
            from("sampleConf/logback.xml") {
                into("config/")
            }
        }
    }
}


/**
 * When the application plugin is applied a distribution named boot is created.
 * This distribution contains the archive produced by the bootJar or bootWar task and scripts
 * to launch it on Unix-like platforms and Windows.
 * Zip and tar distributions can be built by the bootDistZip and bootDistTar tasks respectively.
 * distZip and distTar create dists and tars w/o a single executable.
 *
 */


dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

tasks.getByName<BootJar>("bootJar") {
    mainClassName = "io.nats.bridge.admin.ApplicationMain"
    manifest {
        attributes("Start-Class" to "io.nats.bridge.admin.ApplicationMain")
    }
    launchScript()

    archiveClassifier.set("boot")
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.getByName<CreateStartScripts>("startScripts") {
    val gen = unixStartScriptGenerator as org.gradle.api.internal.plugins.UnixStartScriptGenerator
    gen.template = resources.text.fromFile(file("src/main/bash/unix.txt"))
}

tasks {


    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    withType<Test> {
        useJUnitPlatform()
    }
    withType<ProcessResources> {
        filesMatching("application.properties") {
            expand(project.properties)
        }
    }


    create<JavaExec>("runIntegrationNatsToNats") {
        main = "io.nats.bridge.admin.integration.IntegrationRequestReplyMainNatstoNats"
        classpath = sourceSets["main"].runtimeClasspath
//        environment(mapOf(
//                "NATS_BRIDGE_KEY_PASS_ENV" to "Y2xvdWR1cmFibGUx",
//                "NATS_BRIDGE_TRUST_PASS_ENV" to "Y2xvdWR1cmFibGUy"
//        ))
    }



    create<JavaExec>("runIntegrationNatsToMQ") {
        main = "io.nats.bridge.admin.integration.IntegrationRequestReplyMain"
        classpath = sourceSets["main"].runtimeClasspath
        environment(mapOf(
            "NATS_BRIDGE_KEY_PASS_ENV" to "Y2xvdWR1cmFibGUx",
            "NATS_BRIDGE_TRUST_PASS_ENV" to "Y2xvdWR1cmFibGUy"
        ))
    }



    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.spring.io/snapshot")
        maven("https://repo.spring.io/milestone")
    }

    dependencyManagement {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.11.2")
        }
    }



    dependencies {

        // Kotlin
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.jetbrains.kotlin:kotlin-reflect")

        // Logging
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("org.slf4j:slf4j-api:1.7.25")
        implementation("net.logstash.logback:logstash-logback-encoder:4.11")
        implementation("ch.qos.logback:logback-core:1.2.3")

        // Spring Boot
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        //implementation( "org.springframework.metrics:spring-metrics")

        // Jackson
        implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

        // Security
        implementation("org.springframework.boot:spring-boot-starter-security")

        // Swagger
        implementation("io.springfox:springfox-swagger-ui:2.7.0")
        implementation("io.springfox:springfox-swagger2:2.7.0")
        implementation("io.nats.bridge:nats-jms-bridge:1.0.0")
        implementation("io.micrometer:micrometer-registry-prometheus:1.3.6")

        // https://mvnrepository.com/artifact/com.ibm.mq/com.ibm.mq.allclient
        implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.5.0")

        // Testing
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(module = "junit-vintage-engine")
        }
        testImplementation("org.junit.jupiter:junit-jupiter-api")
        testImplementation("org.junit.jupiter:junit-jupiter-engine")

        implementation("io.jsonwebtoken:jjwt-api:0.11.1")
        implementation("io.jsonwebtoken:jjwt-impl:0.11.1")
        implementation("io.jsonwebtoken:jjwt-jackson:0.11.1")
        implementation("io.nats:jnats:2.11.4")


        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        implementation("com.squareup.okhttp3:okhttp:4.5.0")


        implementation("com.github.ajalt:clikt:2.7.1")

        implementation("io.nats.bridge:nats-jms-bridge-message:1.0.0" + libType)
        implementation("io.nats.bridge:nats-jms-bridge:1.0.0" + libType)

    }

//    tasks.getByName<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
//        main = "io.nats.bridge.admin.NATSJmsBridgeApplication"
//        environment(mapOf(
//                "NATS_BRIDGE_KEY_PASS_ENV" to "Y2xvdWR1cmFibGUx",
//                "NATS_BRIDGE_TRUST_PASS_ENV" to "Y2xvdWR1cmFibGUy"
//        ))
//    }
}
