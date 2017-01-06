package io.quartic.weyl.core.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Geometry;
import org.junit.Test;

import java.io.IOException;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static io.quartic.weyl.core.geojson.UtilsKt.toJts;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class UtilsShould {
    private final ObjectMapper OM = objectMapper();

    @Test
    public void parse_multiline_string() throws IOException {
        String json = "  { \"type\": \"MultiLineString\"," +
                "    \"coordinates\": [" +
                "        [ [100.0, 0.0], [101.0, 1.0] ]," +
                "        [ [102.0, 2.0], [103.0, 3.0] ]" +
                "      ]" +
                "    }";

        com.vividsolutions.jts.geom.Geometry geometry = toJts(OM.readValue(json, Geometry.class));
        assertThat(geometry.getGeometryType(), equalTo("MultiLineString"));

        MultiLineString multiLineString = (MultiLineString) geometry;
        assertThat(geometry.getNumGeometries(), equalTo(2));

        LineString lineString = (LineString) multiLineString.getGeometryN(0);
        Coordinate[] coordinates = lineString.getCoordinates();
        assertThat(coordinates, equalTo(new Coordinate[]{new Coordinate(100.0, 0.0), new Coordinate(101.0, 1.0)}));

        LineString lineString2 = (LineString) multiLineString.getGeometryN(1);
        Coordinate[] coordinates2 = lineString2.getCoordinates();
        assertThat(coordinates2, equalTo(new Coordinate[]{new Coordinate(102.0, 2.0), new Coordinate(103.0, 3.0)}));
    }

    @Test
    public void parse_crs_in_feature_collection() throws IOException {
        String json = "{ \"type\":\"FeatureCollection\",  " +
        "\"crs\": {" +
                "    \"type\": \"name\"," +
                "    \"properties\": {" +
                "      \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\"" +
                "      }" +
                "    }, " +
                " \"features\": []" +
        "}";

         FeatureCollection featureCollection = OM.readValue(json, FeatureCollection.class);
        assertThat(featureCollection.getFeatures().size(), equalTo(0));
    }

    @Test
    public void null_properties_in_feature_shouldnt_throw() throws IOException {
        String json = " { \"type\": \"Feature\",\n" +
                "    \"bbox\": [-10.0, -10.0, 10.0, 10.0],\n" +
                "    \"properties\": {\"test\": null}," +
                "    \"geometry\": {\n" +
                "      \"type\": \"Polygon\",\n" +
                "      \"coordinates\": [[\n" +
                "        [-10.0, -10.0], [10.0, -10.0], [10.0, 10.0], [-10.0, 10.0]\n" +
                "        ]]\n" +
                "      }\n" +
                "    }";
        Feature feature = OM.readValue(json, Feature.class);
        assertThat(feature.getProperties().values(), containsInAnyOrder((String)null));
    }
}
