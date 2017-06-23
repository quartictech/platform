package io.quartic.howl.storage

import io.quartic.howl.storage.Storage.PutResult
import java.io.InputStream

class RoutingStorage(configs: Map<String, StorageConfig>) : Storage {
    // TODO - this is a pain to test
    private val gcsFactory = GcsStorageFactory()
    private val dests = configs.mapValues {
        val config = it.value
        when (config) {
            is LocalStorage.Config -> LocalStorage(config)
            is GcsStorageFactory.Config -> gcsFactory.create(config)
            is S3Storage.Config -> S3Storage(config)
            else -> throw RuntimeException("Unrecognised storage type '${config.javaClass}'")
        }
    }

    override fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType? {
        return dests[coords.targetNamespace]?.getData(coords, version)
    }
    
    override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): PutResult? {
        return dests[coords.targetNamespace]?.putData(coords, contentType, inputStream)
    }
}
