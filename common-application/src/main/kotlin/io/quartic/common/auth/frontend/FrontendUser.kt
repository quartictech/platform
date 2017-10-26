package io.quartic.common.auth.frontend

import io.quartic.common.model.CustomerId
import java.security.Principal

// TODO - make params integers?
data class FrontendUser(val id: String, val customerId: CustomerId) : Principal {
    constructor(id: Long, customerId: CustomerId) : this(id.toString(), customerId)
    constructor(id: Long, customerId: Long) : this(id.toString(), CustomerId(customerId))

    override fun getName() = id
}
