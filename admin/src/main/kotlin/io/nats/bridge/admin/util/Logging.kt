package io.nats.bridge.admin.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getLogger(): Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)