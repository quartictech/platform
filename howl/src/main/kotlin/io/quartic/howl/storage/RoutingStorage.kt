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

    override fun getObject(coords: StorageCoords) =
        dests[coords.targetNamespace]?.getObject(coords)

    override fun getMetadata(coords: StorageCoords) =
        dests[coords.targetNamespace]?.getMetadata(coords)

    override fun putObject(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream) =
        dest(coords)?.putObject(coords, contentLength, contentType, inputStream) ?: false

    // TODO - enforce matching targetNamespace (maybe restructure StorageCoords accordingly)
    override fun copyObject(source: StorageCoords, dest: StorageCoords) =
        dests[source.targetNamespace]?.copyObject(source, dest)

    private fun dest(coords: StorageCoords): Storage? {
        val dest = dests[coords.targetNamespace]
        if (dest == null) {
            LOG.warn("Unknown namespace '${coords.targetNamespace}'")
        }
        return dest
    }
}
