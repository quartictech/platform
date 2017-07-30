package io.quartic.howl

import io.quartic.common.application.ConfigurationBase

data class HowlConfiguration(
        val storage: StorageBackendConfig
) : ConfigurationBase()
