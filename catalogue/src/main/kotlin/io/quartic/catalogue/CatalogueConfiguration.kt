package io.quartic.catalogue

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.db.DatabaseConfiguration

data class CatalogueConfiguration(val database: DatabaseConfiguration) : ConfigurationBase()
