package io.nats.bridge.admin.importer

import io.nats.bridge.admin.ConfigRepo
import io.nats.bridge.admin.models.bridges.*
import java.io.File

class BridgeDelimImporterException(message: String) : Exception(message)

object BridgeFileDelimImporterUtils {

    private const val BRIDGE_NAME = 0
    private const val BRIDGE_TYPE = 1
    private const val SOURCE_NAME = 2
    private const val SOURCE_TYPE = 3
    private const val SOURCE_SUBJECT = 4
    private const val SOURCE_CLUSTER = 5
    private const val DESTINATION_NAME = 6
    private const val DESTINATION_TYPE = 7
    private const val DESTINATION_SUBJECT = 8
    private const val DESTINATION_CLUSTER = 9
    private const val BRIDGE_TYPE_REQUEST_REPLY = "r"
    private const val BRIDGE_TYPE_REQUEST_FORWARD = "f"
    private val VALID_BRIDGE_TYPES = setOf(BRIDGE_TYPE_REQUEST_FORWARD, BRIDGE_TYPE_REQUEST_REPLY)
    private const val BRIDGE_TYPE_JMS = "j"
    private const val BRIDGE_TYPE_NATS = "n"
    private val VALID_BUS_TYPES = setOf(BRIDGE_TYPE_JMS, BRIDGE_TYPE_NATS)

    fun parseLine(line: String, clusterConfigs: Map<String, Cluster>, delim: String = "\t"): MessageBridgeInfo {
        val parts = line.split(delim).map { it.trim() }.filter { !it.isBlank() }.toList()
        if (parts.size != 10) throw BridgeDelimImporterException("Line must have ten cells but only has a size of ${parts.size}")
        val name = parts[BRIDGE_NAME]
        val sBridgeType = parts[BRIDGE_TYPE]
        val sourceName = parts[SOURCE_NAME]
        val sSourceType = parts[SOURCE_TYPE]
        val sourceSubject = parts[SOURCE_SUBJECT]
        val sourceClusterName = parts[SOURCE_CLUSTER]
        val destName = parts[DESTINATION_NAME]
        val sDestType = parts[DESTINATION_TYPE]
        val destSubject = parts[DESTINATION_SUBJECT]
        val destClusterName = parts[DESTINATION_CLUSTER]

        if (!VALID_BRIDGE_TYPES.contains(sBridgeType))
            throw BridgeDelimImporterException("bridge $name has illegal bridge type, " +
                    "$sBridgeType not in (r, f), $parts")
        if (!VALID_BUS_TYPES.contains(sSourceType))
            throw BridgeDelimImporterException("bridge $name has illegal source bus type, " +
                    "$sSourceType not in (n, j), $parts")
        if (!VALID_BUS_TYPES.contains(sDestType))
            throw BridgeDelimImporterException("bridge $name has illegal destination bus type, " +
                    "$sDestType not in (n, j), $parts")
        if (!clusterConfigs.containsKey(sourceClusterName))
            throw BridgeDelimImporterException("bridge $name has a source $sourceName for subject $sourceSubject " +
                    "cluster name $sourceClusterName that does not exist, $parts")
        if (!clusterConfigs.containsKey(destClusterName))
            throw BridgeDelimImporterException("bridge $name has a source $destName for subject $destSubject " +
                    "cluster name $destClusterName that does not exist, $parts")

        val bridgeType = if (sBridgeType == BRIDGE_TYPE_REQUEST_REPLY) BridgeType.REQUEST_REPLY else BridgeType.FORWARD
        val sourceType = if (sSourceType == BRIDGE_TYPE_JMS) BusType.JMS else BusType.NATS
        val destType = if (sDestType == BRIDGE_TYPE_JMS) BusType.JMS else BusType.NATS
        val sourceBus = MessageBusInfo(name = sourceName, busType = sourceType, clusterName = sourceClusterName, subject = sourceSubject)
        val destBus = MessageBusInfo(name = destName, busType = destType, clusterName = destClusterName, subject = destSubject)
        return MessageBridgeInfo(name = name, bridgeType = bridgeType, source = sourceBus, destination = destBus)
    }
}


class BridgeFileDelimImporter(private val configRepo: ConfigRepo, private val delim: String = "\t") : BridgeFileImporter {

    fun transform(inputFile: File, clusterConfigs: Map<String, Cluster>): List<MessageBridgeInfo> {
        return inputFile.readLines().toList().map { it.trim() }.filter { !it.startsWith("#") }.map {
            BridgeFileDelimImporterUtils.parseLine(it, clusterConfigs, delim)
        }
    }

    override fun import(inputFile: File) = transform(inputFile, configRepo.readClusterConfigs())
            .forEach { configRepo.addBridge(it) }

}