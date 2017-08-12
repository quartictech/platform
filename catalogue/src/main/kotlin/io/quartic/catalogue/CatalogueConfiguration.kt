package io.quartic.catalogue

import io.quartic.common.application.ConfigurationBase

data class CatalogueConfiguration(val backend: StorageBackendConfig) : ConfigurationBase()
