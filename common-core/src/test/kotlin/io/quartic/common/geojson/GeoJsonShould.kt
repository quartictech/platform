package io.quartic.common.geojson

import com.fasterxml.jackson.databind.SerializationFeature
import io.quartic.common.serdes.objectMapper
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import java.io.StringWriter

class GeoJsonShould {

    @Test
    fun deserializeBackToOriginal() {
        val original = FeatureCollection(listOf(
                Feature(geometry = Point(listOf(102.0, 0.5))),
                Feature(geometry = LineString(listOf(
                                listOf(102.0, 0.0),
                                listOf(103.0, 1.0),
                                listOf(104.0, 0.0),
                                listOf(105.0, 1.0)
                        ))),
                Feature(geometry = Polygon(listOf(listOf(
                                listOf(100.0, 0.0),
                                listOf(101.0, 0.0),
                                listOf(101.0, 1.0),
                                listOf(100.0, 1.0),
                                listOf(100.0, 0.0)
                        ))))
        ))

        objectMapper().enable(SerializationFeature.INDENT_OUTPUT)

        val sw = StringWriter()
        objectMapper().writeValue(sw, original)

        val json = sw.toString()

        Assert.assertThat(objectMapper().readValue(json, FeatureCollection::class.java), Matchers.equalTo(original))
    }
}
