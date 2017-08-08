package io.quartic.common.auth

import io.quartic.common.model.CustomerId
import java.security.Principal

// TODO - make params integers?
data class User @JvmOverloads constructor(
    val id: String,
    val customerId: CustomerId? = null // TODO - make non-nullable once we eliminate DummyAuth
) : Principal {
    constructor(id: Long, customerId: CustomerId) : this(id.toString(), customerId)
    constructor(id: Long, customerId: Long) : this(id.toString(), CustomerId(customerId))

    override fun getName() = id
}
