package io.quartic.howl.storage

import java.io.IOException
import java.io.InputStream
import java.time.Instant

interface Storage {
    data class PutResult(val version: Long?)

    data class StorageMetadata(
        val lastModified: Instant,
        val contentType: String,
        val contentLength: Long
    )

    @Throws(IOException::class)
    fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    @Throws(IOException::class)
    fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): PutResult?

    @Throws(IOException::class)
    fun getMetadata(coords: StorageCoords): StorageMetadata?
}
