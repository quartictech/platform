package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter

class NoobAuthFilter : AuthFilter<NoobCredentials, User>() {
    override fun filter(requestContext: javax.ws.rs.container.ContainerRequestContext) {
        val user = requestContext.headers.getFirst("X-Forwarded-User") ?: io.quartic.common.auth.NoobAuthFilter.Companion.DEFAULT_USER
        if (!authenticate(requestContext, NoobCredentials(user), javax.ws.rs.core.SecurityContext.BASIC_AUTH)) {
            throw javax.ws.rs.WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm))
        }
    }

    companion object {
        fun create() = object : AuthFilterBuilder<NoobCredentials, User, io.quartic.common.auth.NoobAuthFilter>() { override fun newInstance() = io.quartic.common.auth.NoobAuthFilter() }
                .setAuthenticator { (id) -> java.util.Optional.of(User(id)) }
                .buildAuthFilter()

        val DEFAULT_USER = "default"
    }
}