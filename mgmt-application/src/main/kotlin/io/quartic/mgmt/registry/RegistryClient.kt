package io.quartic.mgmt.registry

import io.quartic.mgmt.registry.model.Customer

interface RegistryClient {
    fun getCustomerBySubdomain(subdomain: String): Customer?
}
