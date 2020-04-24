package nats.io.nats.bridge.admin.importer

import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.BridgeType
import nats.io.nats.bridge.admin.models.bridges.BusType
import nats.io.nats.bridge.admin.models.bridges.Cluster
import nats.io.nats.bridge.admin.repos.ConfigRepoFromFiles
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

internal class BridgeFileDelimImporterTest {

    var fileConf: File? = null
    var fileInput: File? = null
    var configMap: Map<String, Cluster>? = null
    var configRepo : ConfigRepo? = null

    val inputStr = """
        # JMS to Nats Bridge
        jms To Nats| r | jms Bar | j | queue/barQueue | activeMQTest| nats Foo | n | fooSubject| natsTest
        # Nats To JMS Bridge
        Nats To JMS| f | nat Bar | n | natsBarSubject | natsTest    | jms Foo  | j | queue/Foo | activeMQTest
    """.trimIndent().replace("|", "\t")

    @BeforeEach
    fun before() {
        fileConf = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        fileInput = File("./data/" + UUID.randomUUID().toString() + "+.tsv")
        fileInput?.writeText(inputStr)
        configRepo = ConfigRepoFromFiles()
        configMap = configRepo?.readConfig()?.clusters

        println(inputStr.replace("\t",  "<TAB>"))
    }

    @AfterEach
    fun after() {
        fileConf?.delete()
        fileInput?.delete()
    }

    @Test
    fun transform() {
        val importer = BridgeFileDelimImporter(configRepo = configRepo!!)
        val bridges = importer.transform(fileInput!!, configMap!!)

        var messageBridge = bridges[0]

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

        messageBridge = bridges[1]
        assertEquals("Nats To JMS", messageBridge.name)
        assertEquals(BridgeType.FORWARD, messageBridge.bridgeType)

        assertEquals("nat Bar", messageBridge.source.name)
        assertEquals(BusType.NATS, messageBridge.source.busType)
        assertEquals("natsBarSubject", messageBridge.source.subject)
        assertEquals("natsTest", messageBridge.source.clusterName)

        assertEquals("jms Foo", messageBridge.destination.name)
        assertEquals(BusType.JMS, messageBridge.destination.busType)
        assertEquals("queue/Foo", messageBridge.destination.subject)
        assertEquals("activeMQTest", messageBridge.destination.clusterName)
    }

}