package io.quartic.common.auth

import java.security.Principal

data class User(private val id: String) : Principal {
    override fun getName() = id
}