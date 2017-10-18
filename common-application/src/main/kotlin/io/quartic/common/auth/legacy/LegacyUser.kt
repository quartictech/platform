package io.quartic.common.auth.legacy

import java.security.Principal

data class LegacyUser constructor(val id: String) : Principal {
    override fun getName() = id
}
