package io.nats.bridge.admin.repos

import io.nats.bridge.admin.RepoException
import io.nats.bridge.admin.models.bridges.defaultDataModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class ConfigRepoFromFilesRepoTest {

    @Test
    fun readConfig() {
        val file = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        val readConfig = ConfigRepoFromFiles(file).readConfig()
        assertNotNull(readConfig)
        file.delete()
    }

    @Test
    fun addBridge() {
        val file = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        val repo = ConfigRepoFromFiles(file)
        val readConfig = repo.readConfig()
        assertNotNull(readConfig)

        val data = defaultDataModel.copy()
        val bridge = data.bridges.find { it.name == "jmsToNatsSample" }!!
        repo.addBridge(bridge.copy(name = "newBridge"))

        val newConfig = repo.readConfig()

        assertTrue(newConfig.bridges.find { it.name == "newBridge" } != null)
        file.delete()
    }

    @Test
    fun addBridgeErrorSourceClusterDoesNotExist() {
        val file = File("./data/" + UUID.randomUUID().toString() + "+.yaml")
        val repo = ConfigRepoFromFiles(file)
        val readConfig = repo.readConfig()
        assertNotNull(readConfig)

        val data = defaultDataModel.copy()
        val bridge = data.bridges.find { it.name == "jmsToNatsSample" }!!

        try {
            repo.addBridge(bridge.copy(name = "newBridge", source = bridge.source.copy(clusterName = "myleftToe")))
            assertFalse(true)
        } catch (ex: RepoException) {
            //pass
        }

        file.delete()
    }
}