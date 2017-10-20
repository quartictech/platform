package io.quartic.common.auth.legacy

import io.quartic.common.auth.AuthStrategy
import javax.ws.rs.container.ContainerRequestContext

class LegacyAuthStrategy : AuthStrategy<String, LegacyUser> {
    override val principalClass = LegacyUser::class.java
    override val scheme = "Basic"

    override fun extractCredentials(requestContext: ContainerRequestContext)
        = requestContext.headers.getFirst("X-Forwarded-User") ?: "default"

    override fun authenticate(creds: String) = LegacyUser(creds)
}
