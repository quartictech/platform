package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.AuthFilter.AuthFilterBuilder
import java.security.Principal
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext.BASIC_AUTH

fun <C, U : Principal> createAuthFilter(strategy: AuthStrategy<C, U>): AuthFilter<C, U> {
    val builder = object : AuthFilterBuilder<C, U, AuthFilter<C, U>>() {
        override fun newInstance() = object : AuthFilter<C, U>() {
            override fun filter(requestContext: ContainerRequestContext) {
                val creds = strategy.extractCredentials(requestContext)
                if (!authenticate(requestContext, creds, BASIC_AUTH)) {
                    throw WebApplicationException(unauthorizedHandler.buildResponse(strategy.scheme, realm))
                }
            }
        }
    }

    return builder
        .setAuthenticator { creds -> Optional.ofNullable(strategy.authenticate(creds)) }
        .buildAuthFilter()
}

