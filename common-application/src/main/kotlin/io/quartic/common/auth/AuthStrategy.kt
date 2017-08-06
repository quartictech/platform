package io.quartic.common.auth

import javax.ws.rs.container.ContainerRequestContext

interface AuthStrategy<C> {
    val scheme: String
    fun extractCredentials(requestContext: ContainerRequestContext): C?
    fun authenticate(creds: C): User?
}
