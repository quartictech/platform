package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.AuthFilter.AuthFilterBuilder
import io.quartic.common.application.AuthConfiguration
import io.quartic.common.application.FrontendAuthConfiguration
import io.quartic.common.application.InternalAuthConfiguration
import io.quartic.common.application.LegacyAuthConfiguration
import io.quartic.common.auth.frontend.FrontendAuthStrategy
import io.quartic.common.auth.internal.InternalAuthStrategy
import io.quartic.common.auth.legacy.LegacyAuthStrategy
import io.quartic.common.secrets.SecretsCodec
import java.security.Principal
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext.BASIC_AUTH

fun AuthConfiguration.createStrategy(secretsCodec: SecretsCodec) = when (this) {
    is FrontendAuthConfiguration -> FrontendAuthStrategy(this, secretsCodec)
    is InternalAuthConfiguration -> InternalAuthStrategy(this, secretsCodec)
    is LegacyAuthConfiguration -> LegacyAuthStrategy()
}

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

