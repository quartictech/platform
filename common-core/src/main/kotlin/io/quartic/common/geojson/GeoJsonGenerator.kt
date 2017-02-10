package io.quartic.common.geojson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import io.quartic.common.serdes.objectMapper
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream

class GeoJsonGenerator(outputStream: OutputStream) {
    val jsonGenerator : JsonGenerator = JsonFactory().createGenerator(outputStream)

    init {
        jsonGenerator.codec = objectMapper()
    }

    private fun writePreamble() {
        jsonGenerator.writeStartObject()
        jsonGenerator.writeStringField("type", "FeatureCollection")
        jsonGenerator.writeArrayFieldStart("features")
    }

    private fun writePostamble() {
        jsonGenerator.writeEndArray()
        jsonGenerator.writeEndObject()
        jsonGenerator.flush()
    }

    fun writeFeatures(features: Stream<Feature>): Int {
        val count = AtomicLong()
        writePreamble()
        features.forEach { feature ->
            count.incrementAndGet()
            jsonGenerator.writeObject(feature)
        }

        writePostamble()
        return count.toInt()
    }
}
