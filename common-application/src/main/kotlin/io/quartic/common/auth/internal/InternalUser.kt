package io.quartic.common.auth.internal

import java.security.Principal

data class InternalUser(
    val id: String,                 // TODO - what does this ID actually represent?
    val namespaces: List<String>
) : Principal {
    override fun getName() = id
}
