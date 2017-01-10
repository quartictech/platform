package io.quartic.management.conversion;

import com.google.common.collect.ImmutableMap;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Point;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappersKt.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.Assert.assertEquals;

public class CsvConverterShould {
    private final CsvConverter converter = new CsvConverter();

    @Test
    public void handle_windows_line_endings() throws Exception {
        assertProducesCorrectGeoJson("lat,lon,stuff\r\n1,2,foo\r\n3,4,bar");
    }

    @Test
    public void handle_unix_line_endings() throws Exception {
        assertProducesCorrectGeoJson("lat,lon,stuff\n1,2,foo\n3,4,bar");
    }

    private void assertProducesCorrectGeoJson(String csv) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        converter.convert(toInputStream(csv, UTF_8), baos);

        final FeatureCollection featureCollection = decode(baos.toString(UTF_8), FeatureCollection.class);

        assertEquals(featureCollection, new FeatureCollection(newArrayList(
                new Feature(null, new Point(newArrayList(2.0, 1.0)), ImmutableMap.of("stuff", "foo")),
                new Feature(null, new Point(newArrayList(4.0, 3.0)), ImmutableMap.of("stuff", "bar"))
        )));
    }
}
