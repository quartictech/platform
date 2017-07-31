package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.AuthFilter.AuthFilterBuilder
import io.quartic.common.application.AuthConfiguration
import io.quartic.common.application.DummyAuthConfiguration
import io.quartic.common.application.TokenAuthConfiguration
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext.BASIC_AUTH

fun createAuthFilter(config: AuthConfiguration): AuthFilter<*, User> =
    when (config) {
        is DummyAuthConfiguration -> createAuthFilter(DummyAuthStrategy())
        is TokenAuthConfiguration -> createAuthFilter(TokenAuthStrategy(config))
    }

private fun <C> createAuthFilter(strategy: AuthStrategy<C>): AuthFilter<C, User> {
    val builder = object : AuthFilterBuilder<C, User, AuthFilter<C, User>>() {
        override fun newInstance() = object : AuthFilter<C, User>() {
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

