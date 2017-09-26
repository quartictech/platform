package io.quartic.howl.storage

import io.quartic.howl.api.model.StorageMetadata
import java.io.IOException
import java.io.InputStream

interface Storage {

    data class StorageResult(val metadata: StorageMetadata, val inputStream: InputStream) : AutoCloseable {
        override fun close() = inputStream.close()
    }

    @Throws(IOException::class)
    fun getObject(coords: StorageCoords): StorageResult?

    @Throws(IOException::class)
    fun getMetadata(coords: StorageCoords): StorageMetadata?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    @Throws(IOException::class)
    fun putObject(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean
}
