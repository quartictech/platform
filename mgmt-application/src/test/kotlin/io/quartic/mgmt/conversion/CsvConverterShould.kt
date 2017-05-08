package io.quartic.mgmt.conversion

import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.FeatureCollection
import io.quartic.common.geojson.Point
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvConverterShould {
    @Test
    fun convert_properly() {
        val raw = """
            foo,bar,lat,lon,baz
            a,b,1.0,2.0,c
            d,e,3.0,4.0,f
        """.trimIndent()

        val os = ByteArrayOutputStream()

        CsvConverter().convert(raw.byteInputStream(), os)

        val featureCollection = OBJECT_MAPPER.readValue(os.toString(), FeatureCollection::class.java)

        assertThat(featureCollection, equalTo(FeatureCollection(listOf(
                Feature(null, Point(listOf(2.0, 1.0)), mapOf("foo" to "a", "bar" to "b", "baz" to "c")),
                Feature(null, Point(listOf(4.0, 3.0)), mapOf("foo" to "d", "bar" to "e", "baz" to "f"))
        ))))
    }
}