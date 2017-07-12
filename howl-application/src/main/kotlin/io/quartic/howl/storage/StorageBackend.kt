package io.quartic.howl.storage

import java.io.IOException
import java.io.InputStream

interface StorageBackend {
    @Throws(IOException::class)
    fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType?

    @Throws(IOException::class)
    fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): Long?
}
