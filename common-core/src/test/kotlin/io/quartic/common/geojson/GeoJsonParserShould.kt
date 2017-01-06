package io.quartic.common.geojson

import io.quartic.common.serdes.OBJECT_MAPPER
import org.glassfish.grizzly.utils.Charsets.UTF8_CHARSET
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream

class GeoJsonParserShould {
    @Test
    fun parse_valid_geojson() {
        val parser = GeoJsonParser(ByteArrayInputStream(GEOJSON.toByteArray(UTF8_CHARSET)))
        val featureCollection = OBJECT_MAPPER.readValue(GEOJSON, FeatureCollection::class.java)

        assertThat(parser.asSequence().toList(), equalTo(featureCollection.features))
    }

    @Test
    fun not_require_type() {
        val input = "  { " +
                "    \"features\": [\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\"type\": \"Point\", \"coordinates\": [102.0, 0.5]},\n" +
                "        \"properties\": {\"prop0\": \"value0\"}\n" +
                "        }\n" +
                "       ]\n" +
                "     }"
        GeoJsonParser(ByteArrayInputStream(input.toByteArray(UTF8_CHARSET))).validate()
    }

    @Test
    fun not_barf_on_crs() {
        val input = "  { \"type\": \"FeatureCollection\",\n" +
                " \"crs\": {}, " +
                "    \"features\": [\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\"type\": \"Point\", \"coordinates\": [102.0, 0.5]},\n" +
                "        \"properties\": {\"prop0\": \"value0\"}\n" +
                "        },\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\n" +
                "          \"type\": \"LineString\",\n" +
                "          \"coordinates\": [\n" +
                "            [102.0, 0.0], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]\n" +
                "            ]\n" +
                "          },\n" +
                "        \"properties\": {\n" +
                "          \"prop0\": \"value0\",\n" +
                "          \"prop1\": 0.0\n" +
                "          }\n" +
                "        },\n" +
                "      { \"type\": \"Feature\",\n" +
                "         \"geometry\": {\n" +
                "           \"type\": \"Polygon\",\n" +
                "           \"coordinates\": [\n" +
                "             [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],\n" +
                "               [100.0, 1.0], [100.0, 0.0] ]\n" +
                "             ]\n" +
                "         },\n" +
                "         \"properties\": {\n" +
                "           \"prop0\": \"value0\",\n" +
                "           \"prop1\": {\"this\": \"that\"}\n" +
                "           }\n" +
                "         }\n" +
                "       ]\n" +
                "     }"
        val parser = GeoJsonParser(ByteArrayInputStream(input.toByteArray(UTF8_CHARSET)))
        val featureCollection = OBJECT_MAPPER.readValue(input, FeatureCollection::class.java)

        var count = 0
        while (parser.hasNext()) {
            assertThat(parser.next(), equalTo(featureCollection.features[count]))
            count += 1
        }

        assertThat(count, equalTo(featureCollection.features.size))
    }

    companion object {
        private val GEOJSON = "  { \"type\": \"FeatureCollection\",\n" +
                "    \"features\": [\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\"type\": \"Point\", \"coordinates\": [102.0, 0.5]},\n" +
                "        \"properties\": {\"prop0\": \"value0\"}\n" +
                "        },\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\n" +
                "          \"type\": \"LineString\",\n" +
                "          \"coordinates\": [\n" +
                "            [102.0, 0.0], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]\n" +
                "            ]\n" +
                "          },\n" +
                "        \"properties\": {\n" +
                "          \"prop0\": \"value0\",\n" +
                "          \"prop1\": 0.0\n" +
                "          }\n" +
                "        },\n" +
                "      { \"type\": \"Feature\",\n" +
                "         \"geometry\": {\n" +
                "           \"type\": \"Polygon\",\n" +
                "           \"coordinates\": [\n" +
                "             [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],\n" +
                "               [100.0, 1.0], [100.0, 0.0] ]\n" +
                "             ]\n" +
                "         },\n" +
                "         \"properties\": {\n" +
                "           \"prop0\": \"value0\",\n" +
                "           \"prop1\": {\"this\": \"that\"}\n" +
                "           }\n" +
                "         }\n" +
                "       ]\n" +
                "     }"
    }
}
