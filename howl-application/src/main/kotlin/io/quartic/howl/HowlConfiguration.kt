package io.quartic.howl

import io.dropwizard.Configuration
import io.quartic.howl.storage.StorageConfig

data class HowlConfiguration(val namespaces: Set<StorageConfig> = emptySet()) : Configuration()
