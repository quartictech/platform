package io.quartic.howl.storage

import io.quartic.common.logging.logger
import java.io.InputStream

class RoutingStorage(
    gcsFactory: GcsStorageFactory,
    s3Factory: S3StorageFactory,
    configs: Map<String, StorageConfig>
) : Storage {
    private val LOG by logger()

    private val dests = configs.mapValues {
        val config = it.value
        when (config) {
            is LocalStorage.Config -> LocalStorage(config)
            is GcsStorageFactory.Config -> gcsFactory.create(config)
            is S3StorageFactory.Config -> s3Factory.create(config)
            else -> throw RuntimeException("Unrecognised storage type '${config.javaClass}'")
        }
    }

    override fun getData(coords: StorageCoords) =
        dests[coords.targetNamespace]?.getData(coords)

    override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream) =
        dest(coords)?.putData(coords, contentLength, contentType, inputStream) ?: false

    private fun dest(coords: StorageCoords): Storage? {
        val dest = dests[coords.targetNamespace]
        if (dest == null) {
            LOG.warn("Unknown namespace '${coords.targetNamespace}'")
        }
        return dest
    }
}
