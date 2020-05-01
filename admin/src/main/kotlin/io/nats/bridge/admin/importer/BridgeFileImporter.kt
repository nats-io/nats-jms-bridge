package io.nats.bridge.admin.importer

import java.io.File

interface BridgeFileImporter {
    fun import(inputFile: File)
}