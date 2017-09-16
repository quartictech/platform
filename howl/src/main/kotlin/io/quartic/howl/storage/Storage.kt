package io.quartic.howl.storage

import io.quartic.howl.storage.NoobCoords.StorageCoords
import java.io.IOException
import java.io.InputStream

interface Storage {
    data class PutResult(val version: Long?)

    @Throws(IOException::class)
    fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    @Throws(IOException::class)
    fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): PutResult?
}
