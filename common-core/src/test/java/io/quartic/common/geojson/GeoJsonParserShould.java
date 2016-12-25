package io.quartic.common.geojson;

import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class GeoJsonParserShould {
    private static final String GEOJSON = "  { \"type\": \"FeatureCollection\",\n" +
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
                "     }";
    @Test
    public void parse_valid_geojson() throws IOException {

        GeoJsonParser parser = new GeoJsonParser(new ByteArrayInputStream(GEOJSON.getBytes(Charsets.UTF8_CHARSET)));
        FeatureCollection featureCollection = objectMapper().readValue(GEOJSON, FeatureCollection.class);

        int count = 0;
        while (parser.hasNext()) {
            assertThat(parser.next(), equalTo(featureCollection.getFeatures().get(count)));
            count += 1;
        }

        assertThat(count, equalTo(featureCollection.getFeatures().size()));
    }

    @Test
    public void parse_all_features() throws IOException {
        GeoJsonParser parser = new GeoJsonParser(new ByteArrayInputStream(GEOJSON.getBytes(Charsets.UTF8_CHARSET)));
        FeatureCollection featureCollection = objectMapper().readValue(GEOJSON, FeatureCollection.class);

        assertThat(featureCollection.getFeatures(), equalTo(parser.features().collect(toList())));
    }

    @Test
    public void not_require_type() throws IOException {
        String input = "  { " +
                "    \"features\": [\n" +
                "      { \"type\": \"Feature\",\n" +
                "        \"geometry\": {\"type\": \"Point\", \"coordinates\": [102.0, 0.5]},\n" +
                "        \"properties\": {\"prop0\": \"value0\"}\n" +
                "        }\n" +
                "       ]\n" +
                "     }";
        new GeoJsonParser(new ByteArrayInputStream(input.getBytes(Charsets.UTF8_CHARSET))).validate();
    }

    @Test
    public void not_barf_on_crs() throws IOException {
      String input = "  { \"type\": \"FeatureCollection\",\n" +
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
                "     }";
        GeoJsonParser parser = new GeoJsonParser(new ByteArrayInputStream(input.getBytes(Charsets.UTF8_CHARSET)));
        FeatureCollection featureCollection = objectMapper().readValue(input, FeatureCollection.class);

        int count = 0;
        while (parser.hasNext()) {
            assertThat(parser.next(), equalTo(featureCollection.getFeatures().get(count)));
            count += 1;
        }

        assertThat(count, equalTo(featureCollection.getFeatures().size()));
    }
}
