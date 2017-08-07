package io.quartic.registry

import io.quartic.registry.api.RegistryService
import io.quartic.registry.api.model.Customer
import javax.ws.rs.NotFoundException

class RegistryResource(
    private val customers: Set<Customer>
) : RegistryService {
    override fun getCustomer(subdomain: String?, githubRepoId: Long?): Customer {
        val matching = customers
        .filter { subdomain == null || it.subdomain == subdomain }
        .filter { githubRepoId == null || it.githubRepoId == githubRepoId }

        if (matching.isEmpty()) {
            throw NotFoundException("No customer with subdomain '$subdomain' and githubRepoId '${githubRepoId}'")
        }

        return matching.first()
    }
}
