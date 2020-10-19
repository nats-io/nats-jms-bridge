/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java Library project to get you started.
 * For more details take a look at the Java Libraries chapter in the Gradle
 * User Manual available at https://docs.gradle.org/6.3/userguide/java_library_plugin.html
 */

plugins {
    // Apply the java-library plugin to add support for Java Library
    `java-library`
    `maven-publish`
}



repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenLocal()
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.nats.bridge"
            artifactId = "nats-jms-bridge-example-transforms"
            version = "0.27.1-beta24"
            from(components["java"])
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {



    // Use JUnit test framework
    testImplementation("junit:junit:4.12")



    implementation (group= "com.fasterxml.jackson.core", name= "jackson-databind", version= "2.10.3")

    implementation ( "org.slf4j:slf4j-api:[1.7,1.8)")
    testImplementation ("ch.qos.logback:logback-classic:1.1.2")

    implementation("io.nats.bridge:nats-jms-bridge-message:0.27.1-beta24")



}
