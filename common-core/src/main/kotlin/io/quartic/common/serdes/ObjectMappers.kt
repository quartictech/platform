package io.quartic.common.serdes

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException

val OBJECT_MAPPER: ObjectMapper = configureObjectMapper(ObjectMapper())

fun objectMapper() = OBJECT_MAPPER

fun configureObjectMapper(mapper: ObjectMapper): ObjectMapper = mapper
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .setSerializationInclusion(Include.NON_NULL)

fun encode(obj: Any): String {
    try {
        return OBJECT_MAPPER.writeValueAsString(obj)
    } catch (e: JsonProcessingException) {
        throw RuntimeException("Could not encode", e)
    }

}

fun <T> decode(src: String, type: Class<T>): T {
    try {
        return OBJECT_MAPPER.readValue(src, type)
    } catch (e: IOException) {
        throw RuntimeException("Could not decode", e)
    }
}
