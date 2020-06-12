package io.nats.bridge.admin.repos


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.RepoException
import io.nats.bridge.admin.models.bridges.MessageBridgeInfo
import io.nats.bridge.admin.models.bridges.NatsBridgeConfig
import io.nats.bridge.admin.models.bridges.defaultDataModel
import io.nats.bridge.admin.util.ObjectMapperUtils
import io.nats.bridge.admin.util.PathUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.nio.file.Files

class ConfigRepoFromPath(private val configFile: Path = File("./config/nats-bridge.yaml").toPath(),
                         private val mapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()) : ConfigRepo {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init() {
        readConfig()
    }

    override fun readConfig(): NatsBridgeConfig {

        if (!Files.exists(configFile) && Files.isWritable(configFile.parent)) saveConfig(defaultDataModel)
        return mapper.readValue(PathUtils.read(configFile))
    }

    override fun readClusterConfigs() = readConfig().clusters

    override fun addBridge(messageBridge: MessageBridgeInfo) {
        logger.info("Adding Bridge: ${messageBridge.name} ...")

        /* Read in the whole config file. */
        val readConfig = this.readConfig()

        /* See if this bridge exists already, complain if it exists, otherwise add the bridge. */
        if (readConfig.bridges.find { messageBridge.name == it.name } == null) {

            if (!readConfig.clusters.containsKey(messageBridge.source.clusterName)) {
                throw RepoException("The message bridge ${messageBridge.name} source ${messageBridge.source.name} " +
                        "cluster ${messageBridge.source.clusterName} has not been configured yet")
            }
            if (!readConfig.clusters.containsKey(messageBridge.destination.clusterName)) {
                throw RepoException("The message bridge ${messageBridge.name} destination ${messageBridge.destination.name} " +
                        "cluster ${messageBridge.destination.clusterName} has not been configured yet")
            }

            /* Add the bridge. */
            val newConfig = readConfig.copy(bridges = listOf(*readConfig.bridges.toTypedArray(),
                    messageBridge), dateTime = LocalDateTime.now())
            saveConfig(newConfig)
        } else {
            logger.info("Adding Bridge: Name already found ${messageBridge.name} ...")
        }
    }

    override fun saveConfig(conf: NatsBridgeConfig) {
        logger.info("Saving NATS Bridge config... " + LocalDateTime.now())
        Files.createDirectories(configFile.parent)
        Files.writeString(configFile, mapper.writeValueAsString(conf))
    }


}

