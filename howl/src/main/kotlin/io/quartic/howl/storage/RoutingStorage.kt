package io.quartic.howl.storage

import java.io.InputStream

class RoutingStorage(
    gcsFactory: GcsStorageFactory,
    s3Factory: S3StorageFactory,
    configs: Map<String, StorageConfig>
) : Storage {
    private val dests = configs.mapValues {
        val config = it.value
        when (config) {
            is LocalStorage.Config -> LocalStorage(config)
            is GcsStorageFactory.Config -> gcsFactory.create(config)
            is S3StorageFactory.Config -> s3Factory.create(config)
            else -> throw RuntimeException("Unrecognised storage type '${config.javaClass}'")
        }
    }

    override fun getData(coords: StorageCoords, version: Long?)
            = dests[coords.targetNamespace]?.getData(coords, version)

    override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream)
            = dests[coords.targetNamespace]?.putData(coords, contentLength, contentType, inputStream)
}
