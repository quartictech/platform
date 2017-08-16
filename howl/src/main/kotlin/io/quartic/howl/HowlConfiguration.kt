package io.quartic.howl

import io.quartic.common.application.ConfigurationBase
import io.quartic.howl.storage.StorageConfig

data class HowlConfiguration(
        val namespaces: Map<String, StorageConfig> = emptyMap()
) : ConfigurationBase()