package io.quartic.common.geojson

import io.quartic.common.serdes.objectMapper
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.stream.Stream

class GeoJsonGeneratorShould {
    val outputStream = ByteArrayOutputStream()
    val generator = GeoJsonGenerator(outputStream)

    @Test
    fun generate_valid_geosjon() {
        val feature = Feature("id", Point(arrayListOf(0.0, 0.0)), mapOf(Pair("foo", 1)))
        generator.writeFeatures(Stream.of(feature))
        val featureCollection = objectMapper().readValue(outputStream.toByteArray(), FeatureCollection::class.java)
        assertThat(featureCollection, equalTo(FeatureCollection(listOf(feature))))
    }
}
