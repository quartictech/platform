package io.quartic.management.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface GeoJsonConverter {
    void convert(InputStream data, OutputStream outputStream) throws IOException;
}
