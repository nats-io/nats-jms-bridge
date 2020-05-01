package io.nats.bridge.admin.repos


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.RepoException
import io.nats.bridge.admin.models.bridges.MessageBridgeInfo
import io.nats.bridge.admin.models.bridges.NatsBridgeConfig
import io.nats.bridge.admin.models.bridges.defaultDataModel
import io.nats.bridge.admin.util.ObjectMapperUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime

class ConfigRepoFromFiles(private val configFile: File = File("./config/nats-bridge.yaml"),
                          private val mapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()) : ConfigRepo {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init() {
        readConfig()
    }

    override fun readConfig(): NatsBridgeConfig {
        if (!configFile.exists()) saveConfig(defaultDataModel)
        return mapper.readValue(configFile)
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
        logger.info("Saving Nats Bridge config... " + LocalDateTime.now())
        configFile.parentFile.mkdirs()
        mapper.writeValue(configFile, conf)
    }


}

