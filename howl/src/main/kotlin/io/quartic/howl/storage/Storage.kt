package io.quartic.howl.storage

import java.io.IOException
import java.io.InputStream
import java.time.Instant

interface Storage {
    data class StorageMetadata(
        val lastModified: Instant,
        val contentType: String,
        val contentLength: Long
    )

    data class StorageResult(val metadata: StorageMetadata, val inputStream: InputStream) : AutoCloseable {
        override fun close() = inputStream.close()
    }

    @Throws(IOException::class)
    fun getData(coords: StorageCoords): StorageResult?

    @Throws(IOException::class)
    fun getMetadata(coords: StorageCoords): StorageMetadata?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    @Throws(IOException::class)
    fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean
}
