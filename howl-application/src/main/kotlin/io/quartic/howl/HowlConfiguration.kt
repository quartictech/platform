package io.quartic.howl

import io.dropwizard.Configuration

data class HowlConfiguration(
        val storage: StorageBackendConfig
) : Configuration()
