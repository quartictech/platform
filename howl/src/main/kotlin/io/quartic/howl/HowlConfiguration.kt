package io.quartic.howl

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.howl.storage.StorageConfig

data class HowlConfiguration(
    val aws: AwsConfiguration? = null,
    val namespaces: Map<String, StorageConfig> = emptyMap()
) : ConfigurationBase() {
    data class AwsConfiguration(
        val region: String,
        val accessKeyId: String,
        val secretAccessKeyEncrypted: EncryptedSecret
    )
}
