package io.quartic.howl.storage

import io.quartic.howl.api.model.StorageMetadata
import java.io.InputStream

interface Storage {

    data class StorageResult(val metadata: StorageMetadata, val inputStream: InputStream) : AutoCloseable {
        override fun close() = inputStream.close()
    }

    // Null indicates not found, exception indicates some other error
    fun getObject(coords: StorageCoords): StorageResult?

    // Null indicates not found, exception indicates some other error
    fun getMetadata(coords: StorageCoords): StorageMetadata?

    // Exception indicates some other error
    fun putObject(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream)

    // Null indicates source not found, exception indicates some other error
    fun copyObject(source: StorageCoords, dest: StorageCoords): StorageMetadata?
}
