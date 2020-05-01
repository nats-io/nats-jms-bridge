package io.nats.bridge.admin.runner

import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.repos.ConfigRepoFromFiles
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class BridgeRunnerManagerIntegrationTest {

    var fileConf: File? = null
    var configRepo: ConfigRepo? = null

    var manager: BridgeRunnerManager? = null

    @BeforeEach
    fun before() {
        createYamlFile()
        configRepo = ConfigRepoFromFiles(fileConf!!)
        manager = BridgeRunnerManager(configRepo!!)
    }


    @AfterEach
    fun after() {
        fileConf?.delete()
        manager?.stop()
    }

    @Test
    fun testRunnerManager() {
        val m: BridgeRunnerManager = manager!!

        m.init()

        for (x in 0..100) {
            Thread.sleep(100)
            if (m.wasStarted()) break
        }

        assertFalse(m.isStopped())
        assertTrue(m.isRunning())
        assertEquals(null, m.getLastError())
        assertFalse(m.wasError())

        m.stop()

        for (x in 0..100) {
            Thread.sleep(100)
            if (m.isStopped()) break
        }
        assertTrue(m.isStopped())
        assertFalse(m.isRunning())

    }


    private fun createYamlFile() {
        fileConf?.delete()
        bridge0 = bridge0Template()
        clusters = runClusterTemplate()
        configYamlContents = runTemplate()
        fileConf = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        fileConf?.writeText(configYamlContents)
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
      userName: "cloudurable"
      password: "cloudurable"         
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