package io.quartic.howl

import io.dropwizard.Configuration
import io.quartic.howl.storage.GcsStorageBackend

data class HowlConfiguration(
        val localDisk: Boolean = false,
        val gcs: GcsStorageBackend.Config
) : Configuration()
