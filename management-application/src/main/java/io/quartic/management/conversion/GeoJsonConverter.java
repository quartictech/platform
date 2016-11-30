package io.quartic.management.conversion;

import io.quartic.geojson.FeatureCollection;

import java.io.IOException;
import java.io.InputStream;

public interface GeoJsonConverter {
    FeatureCollection convert(InputStream data) throws IOException;
}
