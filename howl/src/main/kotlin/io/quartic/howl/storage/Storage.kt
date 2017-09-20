package io.quartic.howl.storage

import java.io.IOException
import java.io.InputStream

interface Storage {
    @Throws(IOException::class)
    fun getData(coords: StorageCoords): InputStreamWithContentType?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    @Throws(IOException::class)
    fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean
}
