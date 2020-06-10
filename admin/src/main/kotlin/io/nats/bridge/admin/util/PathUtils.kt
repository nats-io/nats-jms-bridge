package io.nats.bridge.admin.util

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object PathUtils {
    fun read(path: Path): String {
        return readFromBuffer(Files.newBufferedReader(path, StandardCharsets.UTF_8))
    }

    private fun readFromBuffer(newBufferedReader: BufferedReader): String {
        return newBufferedReader.readText()
    }
}