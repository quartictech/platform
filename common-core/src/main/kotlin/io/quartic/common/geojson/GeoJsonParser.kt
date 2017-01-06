package io.quartic.common.geojson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.JsonToken.*
import io.quartic.common.geojson.GeoJsonParser.State.*
import io.quartic.common.serdes.OBJECT_MAPPER
import java.io.IOException
import java.io.InputStream

/**
 * GeoJsonParser is a very forgiving parser of GeoJSON files containing a simple FeatureCollection.
 */
class GeoJsonParser constructor(inputStream: InputStream) : Iterator<Feature> {
    internal enum class State {
        START,
        FEATURES,
        END
    }

    private val parser: JsonParser
    private var token: JsonToken? = null
    private var state = START

    init {
        val jsonFactory = JsonFactory()
        jsonFactory.codec = OBJECT_MAPPER
        parser = jsonFactory.createParser(inputStream)
    }

    override fun hasNext(): Boolean {
        state = update()
        return state == FEATURES && token == START_OBJECT
    }

    override fun next(): Feature {
        state = update()
        when (state) {
            FEATURES -> {
                val feature = parser.readValueAs(Feature::class.java)
                token = parser.nextToken()
                return feature
            }
            else -> throw RuntimeException("Internal inconsistency - not parsing FEATURES")
        }
    }

    fun validate() {
        while (hasNext()) {
            next()
        }
    }

    private fun update() = when (state) {
        START -> {
            until(FIELD_NAME, "features")
            expect(START_ARRAY)
            expect(START_OBJECT)
            FEATURES
        }
        FEATURES -> {
            when (token) {
                START_OBJECT -> state
                END_ARRAY -> END
                else -> throw IOException("Unexpected token: " + token!!)
            }
        }
        END -> state
    }

    private fun until(expected: JsonToken, text: String) {
        token = parser.nextToken()
        while (token != null) {
            if (token == expected && parser.text == text) {
                return
            }
            token = parser.nextToken()
        }
        throw IOException("Reached end of input searching for: $expected[text=$text]")
    }

    private fun expect(expected: JsonToken) {
        token = parser.nextToken()
        if (token != expected) {
            throw IOException("Expected $expected but found $token")
        }
    }
}
