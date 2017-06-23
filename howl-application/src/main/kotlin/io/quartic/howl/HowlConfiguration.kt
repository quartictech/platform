package io.quartic.howl

import io.dropwizard.Configuration
import io.quartic.howl.storage.GcsStorage

data class HowlConfiguration(
        val localDisk: Boolean = false,
        val gcs: GcsStorage.Config
) : Configuration()
