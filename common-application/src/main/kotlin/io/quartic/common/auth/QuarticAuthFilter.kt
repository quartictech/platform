package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter

class QuarticAuthFilter : AuthFilter<QuarticCredentials, User>() {
    override fun filter(requestContext: javax.ws.rs.container.ContainerRequestContext) {
        val user = requestContext.headers.getFirst("X-Forwarded-User") ?: io.quartic.common.auth.QuarticAuthFilter.Companion.DEFAULT_USER
        if (!authenticate(requestContext, QuarticCredentials(user), javax.ws.rs.core.SecurityContext.BASIC_AUTH)) {
            throw javax.ws.rs.WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm))
        }
    }

    companion object {
        fun create() = object : AuthFilterBuilder<QuarticCredentials, User, io.quartic.common.auth.QuarticAuthFilter>() { override fun newInstance() = io.quartic.common.auth.QuarticAuthFilter() }
                .setAuthenticator { (id) -> java.util.Optional.of(User(id)) }
                .buildAuthFilter()

        val DEFAULT_USER = "default"
    }
}