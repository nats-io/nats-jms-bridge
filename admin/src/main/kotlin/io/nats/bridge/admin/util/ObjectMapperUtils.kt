package io.nats.bridge.admin.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object ObjectMapperUtils {

    fun getYamlObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper(YAMLFactory())
        mapper.registerModule(KotlinModule())
        mapper.findAndRegisterModules()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return mapper
    }

    fun getJsonObjectMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper()
        mapper.findAndRegisterModules()
        return mapper
    }

}