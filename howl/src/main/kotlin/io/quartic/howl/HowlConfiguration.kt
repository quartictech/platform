package io.quartic.howl

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.howl.storage.StorageConfig

data class HowlConfiguration(
    val s3: S3Configuration? = null,
    val namespaces: Map<String, StorageConfig> = emptyMap()
) : ConfigurationBase() {
    data class S3Configuration(
        val accessKeyId: String,
        val secretAccessKeyEncrypted: EncryptedSecret
    )
}
