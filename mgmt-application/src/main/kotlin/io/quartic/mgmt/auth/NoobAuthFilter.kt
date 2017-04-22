package io.quartic.mgmt.auth

import io.dropwizard.auth.AuthFilter
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext

class NoobAuthFilter : AuthFilter<NoobCredentials, User>() {
    override fun filter(requestContext: ContainerRequestContext) {
        val user = requestContext.headers.getFirst("X-Forwarded-User") ?: DEFAULT_USER
        if (!authenticate(requestContext, NoobCredentials(user), SecurityContext.BASIC_AUTH)) {
            throw WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm))
        }
    }

    companion object {
        fun create() = object : AuthFilterBuilder<NoobCredentials, User, NoobAuthFilter>() { override fun newInstance() = NoobAuthFilter() }
                .setAuthenticator { (id) -> Optional.of(User(id)) }
                .buildAuthFilter()

        val DEFAULT_USER = "default"
    }
}