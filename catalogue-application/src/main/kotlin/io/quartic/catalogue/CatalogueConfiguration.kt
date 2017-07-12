package io.quartic.catalogue

import io.dropwizard.Configuration

data class CatalogueConfiguration(val backend: StorageBackendConfig) : Configuration()
