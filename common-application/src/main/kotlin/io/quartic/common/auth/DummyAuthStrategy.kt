package io.quartic.common.auth

import javax.ws.rs.container.ContainerRequestContext

class DummyAuthStrategy : AuthStrategy<String> {
    override val scheme = "Basic"

    override fun extractCredentials(requestContext: ContainerRequestContext)
        = requestContext.headers.getFirst("X-Forwarded-User") ?: "default"

    override fun authenticate(creds: String) = User(creds, null)
}
