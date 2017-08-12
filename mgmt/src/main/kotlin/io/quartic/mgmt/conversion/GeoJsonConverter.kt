package io.quartic.mgmt.conversion

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface GeoJsonConverter {
    fun convert(data: InputStream, outputStream: OutputStream)
}
