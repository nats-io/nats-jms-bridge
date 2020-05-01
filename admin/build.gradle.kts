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
    id("io.spring.dependency-management") version "1.0.9.RELEASE"

    id("maven-publish")
    application
    distribution
    id("org.springframework.boot") version "2.2.6.RELEASE"


}

springBoot {
    mainClassName = "io.nats.bridge.admin.ApplicationMain"
    buildInfo {
        properties {
            additional = mapOf(
                    "release" to "Alpha1",
                    "author" to "NATS team"
            )
        }
    }
}

application {
    mainClassName = "io.nats.bridge.admin.ApplicationMain"
}

publishing {
    publications {
        create<MavenPublication>("bootJava") {
            artifact(tasks.getByName("bootJar"))
        }
    }
    repositories {
        maven {
            url = uri("https://repo.example.com")
        }
    }
}

distributions {
    main {
        contents {

            from("bin") {
                into("bin")
            }
            from("sampleConf") {
                include("**/**")
                into("config") {
                    include("**/**")
                }
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

version = "0.3.0-ALPHA1"

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


    create<JavaExec>("runIntegration") {
        main = "io.nats.bridge.admin.integration.IntegrationMain"
        classpath = sourceSets["main"].runtimeClasspath
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.spring.io/snapshot")
        maven("https://repo.spring.io/milestone")
    }

    dependencyManagement {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.9.5")
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
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.8")

        // Security
        implementation("org.springframework.boot:spring-boot-starter-security")

        // Swagger
        implementation("io.springfox:springfox-swagger-ui:2.7.0")
        implementation("io.springfox:springfox-swagger2:2.7.0")

        implementation("io.nats.bridge:nats-jms-bridge:0.3.0-ALPHA1")

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
        implementation("io.nats:jnats:2.6.7")


        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        implementation("com.squareup.okhttp3:okhttp:4.5.0")


    }
}
