package io.quartic.common.auth

import javax.ws.rs.container.ContainerRequestContext

interface AuthStrategy<C> {
    fun extractCredentials(requestContext: ContainerRequestContext): C?
    fun authenticate(creds: C): User?
}
