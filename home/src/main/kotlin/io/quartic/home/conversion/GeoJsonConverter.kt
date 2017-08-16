package io.quartic.home.conversion

import java.io.InputStream
import java.io.OutputStream

interface GeoJsonConverter {
    fun convert(data: InputStream, outputStream: OutputStream)
}
