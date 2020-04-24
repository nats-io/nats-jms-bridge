package nats.io.nats.bridge.admin.importer

import nats.io.nats.bridge.admin.models.bridges.BridgeType
import nats.io.nats.bridge.admin.models.bridges.BusType
import nats.io.nats.bridge.admin.models.bridges.Cluster
import nats.io.nats.bridge.admin.repos.ConfigRepoFromFiles
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class BridgeFileDelimImporterUtilsTest {

    var file: File? = null
    var configMap: Map<String, Cluster>? = null

    @BeforeEach
    fun before() {
        file = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        configMap = ConfigRepoFromFiles().readConfig().clusters
    }

    @AfterEach
    fun after() {
        file?.delete()
    }

    @Test
    fun parseLine() {
        val messageBridge = BridgeFileDelimImporterUtils.parseLine(
                "jms To Nats\tr\tjms Bar\tj\tqueue/barQueue\tactiveMQTest\t" +
                        "nats Foo\tn\tfooSubject\tnatsTest",
                configMap!!)
        assertNotNull(messageBridge)
        assertEquals("jms To Nats", messageBridge.name)
        assertEquals(BridgeType.REQUEST_REPLY, messageBridge.bridgeType)
        assertEquals("jms Bar", messageBridge.source.name)
        assertEquals(BusType.JMS, messageBridge.source.busType)
        assertEquals("queue/barQueue", messageBridge.source.subject)
        assertEquals("activeMQTest", messageBridge.source.clusterName)
        assertEquals("nats Foo", messageBridge.destination.name)
        assertEquals(BusType.NATS, messageBridge.destination.busType)
        assertEquals("fooSubject", messageBridge.destination.subject)
        assertEquals("natsTest", messageBridge.destination.clusterName)
    }
}