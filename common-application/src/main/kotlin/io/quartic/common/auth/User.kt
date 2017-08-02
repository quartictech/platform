package io.quartic.common.auth

import java.security.Principal

// TODO - make params integers?
data class User(
    val id: String,
    val customerId: String? = null // TODO - make non-nullable once we eliminate DummyAuth
) : Principal {
    override fun getName() = id
}
