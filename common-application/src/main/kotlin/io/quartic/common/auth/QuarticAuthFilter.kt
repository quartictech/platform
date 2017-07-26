package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext.BASIC_AUTH

class QuarticAuthFilter : AuthFilter<QuarticCredentials, User>() {
    override fun filter(requestContext: ContainerRequestContext) {
        val user = requestContext.headers.getFirst("X-Forwarded-User") ?: DEFAULT_USER
        if (!authenticate(requestContext, QuarticCredentials(user), BASIC_AUTH)) {
            throw WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm))
        }
    }

    companion object {
        fun create() = object : AuthFilterBuilder<QuarticCredentials, User, QuarticAuthFilter>() { override fun newInstance() = QuarticAuthFilter() }
                .setAuthenticator { (id) -> Optional.of(User(id)) }
                .buildAuthFilter()

        val DEFAULT_USER = "default"
    }
}
