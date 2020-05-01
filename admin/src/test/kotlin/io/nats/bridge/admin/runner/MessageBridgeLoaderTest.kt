package io.nats.bridge.admin.runner

import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.nats.support.NatsMessageBusBuilder
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.repos.ConfigRepoFromFiles
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class MessageBridgeLoaderTest {

    var fileConf: File? = null
    var configRepo: ConfigRepo? = null

    @BeforeEach
    fun before() {
        fileConf?.delete()
        bridge0 = bridge0Template()
        clusters = runClusterTemplate()
        configYamlContents = runTemplate()
        fileConf = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        fileConf?.writeText(configYamlContents)
        configRepo = ConfigRepoFromFiles(fileConf!!)
    }

    @AfterEach
    fun after() {
        fileConf?.delete()
    }

    @Test
    fun testLoadWithUsersServersAndPwd() {

        natsCluster = """
# NATS      
  natsTest:
    name: "natsTest"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config: {"foo":"bar"}    
      password: 
        abc123
      userName:
        RickHightower
    """.trimIndent()

        before()
        val bridgeBuilders = MessageBridgeLoaderImpl(configRepo!!).loadBridgeBuilders()

        val mb = bridgeBuilders[0].destBusBuilder
        assertTrue(mb is NatsMessageBusBuilder)
        if (mb is NatsMessageBusBuilder) {
            assertEquals("RickHightower", mb.options.username)
        }

    }

    @Test
    fun testLoad() {
        val bridgeBuilders = MessageBridgeLoaderImpl(configRepo!!).loadBridgeBuilders()

        assertEquals(2, bridgeBuilders.size)

        /*
            - name: "jmsToNatsSample"
            bridgeType: "REQUEST_REPLY"
         */
        val bridge0 = bridgeBuilders[0]
        assertEquals("jmsToNatsSample", bridge0.name)
        assertTrue(bridge0.requestReply)

        /*
            source:
            name: "jms"
            busType: "JMS"
            subject: "dynamicQueues/sample-jms-queue"
            clusterName: "activeMQTest"
            copyHeaders : true
         */
        val src = bridge0.sourceBusBuilder
        assertTrue(src is JMSMessageBusBuilder)
        if (src is JMSMessageBusBuilder) {
            assertEquals("dynamicQueues/sample-jms-queue", src.destinationName)
            assertTrue(src.isCopyHeaders)
        }


        /*
            destination:
            name: "Nats Sample"
            busType: "NATS"
            subject: "sample-nats-subject"
            clusterName: "natsTest"
         */
        val dest = bridge0.destBusBuilder
        assertTrue(dest is NatsMessageBusBuilder)
        if (dest is NatsMessageBusBuilder) {
            assertEquals("sample-nats-subject", dest.subject)
            assertEquals("bar", dest.optionProperties.getProperty("foo"))
        }
    }


    val header = """
name: "Starter Config"
dateTime:
- 2020
- 4
- 23
- 22
- 5
- 37
- 809497000        
    """.trimIndent()


    var source = """
# Source
  source:
    name: "jms"
    busType: "JMS"
    subject: "dynamicQueues/sample-jms-queue"
    clusterName: "activeMQTest"        
    """.trimIndent()

    var destination = """
# Dest
  destination:
    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"       
    """.trimIndent()

    var bridge0 = bridge0Template()


    private fun bridge0Template(): String {
        return """
- name: "jmsToNatsSample"
  bridgeType: "REQUEST_REPLY"
[[SOURCE]]
[[DESTINATION]]
  copyHeaders: true
    """.trimIndent()
                .replace("[[SOURCE]]", source)
                .replace("[[DESTINATION]]", destination)
    }


    var bridge1 = """
- name: "natsToJMS"
  bridgeType: "FORWARD"
  source:
    name: "Nats Sample"
    busType: "NATS"
    subject: "sample-nats-subject"
    clusterName: "natsTest"
  destination:
    name: "jms"
    busType: "JMS"
    subject: "dynamicQueues/sample-jms-queue"
    clusterName: "activeMQTest"
  copyHeaders: true        
    """.trimIndent()

    var jmsCluster = """
## JMS
  activeMQTest:
    name: "activeMQTest"
    properties: !<jms>
      config:
        java.naming.factory.initial: "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
        connectionFactory.ConnectionFactory: "tcp://localhost:61616"
        queue.queue/testQueue: "queue.queue/testQueue=testQueue"
      jmsDestinationType: "QUEUE"        
    """.trimIndent()
    var natsCluster = """
# NATS      
  natsTest:
    name: "natsTest"
    properties: !<nats>
      host: "localhost"
      port: 4222
      servers: []
      config: {"foo":"bar"}            
    """.trimIndent()

    var clusters = runClusterTemplate()

    private fun runClusterTemplate(): String {
        return """
clusters:
[[JMS]]
[[NATS]]
""".trimIndent()
                .replace("[[JMS]]", jmsCluster)
                .replace("[[NATS]]", natsCluster)
    }


    var configYamlContents = runTemplate()

    private fun runTemplate(): String {
        return """
    [[HEADER]]
    bridges:
    [[BRIDGE_0]]
    [[BRIDGE_1]]
    [[CLUSTERS]]
        """.trimIndent()
                .replace("[[HEADER]]", header)
                .replace("[[BRIDGE_0]]", bridge0)
                .replace("[[BRIDGE_1]]", bridge1)
                .replace("[[CLUSTERS]]", clusters)
    }

}