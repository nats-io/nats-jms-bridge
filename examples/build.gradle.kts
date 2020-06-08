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
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:28.2-jre")

    // Use JUnit test framework
    testImplementation("junit:junit:4.12")

    //
    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.5.0")


    implementation( group= "io.nats", name= "jnats", version= "2.6.7")
    implementation( group= "org.apache.activemq", name= "artemis-jms-client-all", version= "2.11.0")
    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.5.0")
    testImplementation(group= "org.apache.activemq", name= "artemis-server", version= "2.11.0")

}
