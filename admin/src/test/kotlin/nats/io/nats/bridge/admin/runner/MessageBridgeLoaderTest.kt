package nats.io.nats.bridge.admin.runner

import nats.io.nats.bridge.admin.ConfigRepo
import nats.io.nats.bridge.admin.models.bridges.Cluster
import nats.io.nats.bridge.admin.repos.ConfigRepoFromFiles
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class MessageBridgeLoaderTest {

    var fileConf: File? = null
    var configRepo : ConfigRepo? = null


    @BeforeEach
    fun before() {
        fileConf = File("./data/" + UUID.randomUUID().toString() + "+.yaml")

    }

    @AfterEach
    fun after() {
        fileConf?.delete()
    }

    @Test
    fun testLoad() {
        val bridge = MessageBridgeLoaderImpl(configRepo!!)

    }

}