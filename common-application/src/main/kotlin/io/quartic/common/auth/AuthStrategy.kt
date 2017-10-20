package io.quartic.common.auth

import java.security.Principal
import javax.ws.rs.container.ContainerRequestContext

interface AuthStrategy<C, U : Principal> {
    val principalClass: Class<U>
    val scheme: String
    fun extractCredentials(requestContext: ContainerRequestContext): C?
    fun authenticate(creds: C): U?
}
