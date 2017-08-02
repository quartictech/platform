package io.quartic.common.auth

import java.security.Principal

// TODO - make params integers?
data class User @JvmOverloads constructor(
    val id: String,
    val customerId: String? = null // TODO - make non-nullable once we eliminate DummyAuth
) : Principal {
    constructor(id: Int, customerId: Int) : this(id.toString(), customerId.toString())

    override fun getName() = id
}
