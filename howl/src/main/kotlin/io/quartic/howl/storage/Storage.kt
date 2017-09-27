package io.quartic.howl.storage

import io.quartic.howl.api.model.StorageMetadata
import java.io.InputStream

interface Storage {

    data class StorageResult(val metadata: StorageMetadata, val inputStream: InputStream) : AutoCloseable {
        override fun close() = inputStream.close()
    }

    fun getObject(coords: StorageCoords): StorageResult?

    fun getMetadata(coords: StorageCoords): StorageMetadata?

    // Null return indicates NotFound (TODO - wtf does that even mean?)
    fun putObject(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean

    fun copyObject(source: StorageCoords, dest: StorageCoords): StorageMetadata?
}
