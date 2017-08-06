package io.quartic.registry

import io.quartic.common.application.ConfigurationBase
import io.quartic.registry.api.model.Customer

data class RegistryConfiguration(
    val customers: Set<Customer>
) : ConfigurationBase()


