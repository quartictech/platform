package io.quartic.howl

import io.quartic.common.logging.logger
import io.quartic.howl.storage.*

class StorageFactory(
    gcsFactory: GcsStorageFactory,
    s3Factory: S3StorageFactory,
    configs: Map<String, StorageConfig>
) {
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

    fun createFor(targetNamespace: String): Storage? {
        val dest = dests[targetNamespace]
        if (dest == null) {
            LOG.warn("Unknown namespace '${targetNamespace}'")
        }
        return dest
    }
}
