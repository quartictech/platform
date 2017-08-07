package io.quartic.registry

import io.quartic.registry.api.RegistryService
import io.quartic.registry.api.model.Customer
import javax.ws.rs.NotFoundException

class RegistryResource(
    private val customers: Set<Customer>
) : RegistryService {
    override fun getCustomerById(customerId: String): Customer =
        customers.find { it.id == customerId.toIntOrNull() }
            ?: throw NotFoundException("No customer with id '$customerId'")

    override fun getCustomer(subdomain: String?) = customers.find { it.subdomain == subdomain }
        ?: throw NotFoundException("No customer with subdomain '$subdomain'")
}
