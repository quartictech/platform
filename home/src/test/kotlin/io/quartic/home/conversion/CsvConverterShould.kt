package io.quartic.home.conversion

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.FeatureCollection
import io.quartic.common.geojson.Point
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvConverterShould {
    private val converter = CsvConverter()

    @Test
    fun convert_multiple_lines() {
        val raw = """
            foo,bar,lat,lon,baz
            a,b,1.0,2.0,c
            d,e,3.0,4.0,f
        """.trimIndent()

        assertProducesCorrectGeoJson(
                raw,
                Feature(null, Point(2.0, 1.0), mapOf("foo" to "a", "bar" to "b", "baz" to "c")),
                Feature(null, Point(4.0, 3.0), mapOf("foo" to "d", "bar" to "e", "baz" to "f"))
        )
    }

    @Test
    fun handle_windows_line_endings() {
        assertProducesCorrectGeoJson(
                "lat,lon,stuff\r\n1,2,foo\r\n3,4,bar",
                Feature(null, Point(2.0, 1.0), mapOf("stuff" to "foo")),
                Feature(null, Point(4.0, 3.0), mapOf("stuff" to "bar"))
        )
    }

    @Test
    fun handle_unix_line_endings() {
        assertProducesCorrectGeoJson(
                "lat,lon,stuff\n1,2,foo\n3,4,bar",
                Feature(null, Point(2.0, 1.0), mapOf("stuff" to "foo")),
                Feature(null, Point(4.0, 3.0), mapOf("stuff" to "bar"))
        )
    }

    private fun assertProducesCorrectGeoJson(csv: String, vararg features: Feature) {
        val baos = ByteArrayOutputStream()

        converter.convert(csv.byteInputStream(), baos)

        val featureCollection = OBJECT_MAPPER.readValue<FeatureCollection>(baos.toString())

        assertThat(featureCollection, equalTo(FeatureCollection(features.asList())))
    }
}
