package io.quartic.common.auth

import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.AuthFilter.AuthFilterBuilder
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.quartic.common.application.AuthConfiguration
import io.quartic.common.application.DummyAuthConfiguration
import io.quartic.common.application.TokenAuthConfiguration
import java.time.Clock
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.SecurityContext.BASIC_AUTH

class DummyAuthFilter : AuthFilter<String, User>() {
    override fun filter(requestContext: ContainerRequestContext) {
        val user = requestContext.headers.getFirst("X-Forwarded-User") ?: DEFAULT_USER
        if (!authenticate(requestContext, user, BASIC_AUTH)) {
            throw WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm))
        }
    }

    companion object {
        val DEFAULT_USER = "default"
    }
}

fun createAuthFilter(config: AuthConfiguration) =
    when (config) {
        is DummyAuthConfiguration -> createDummyAuthFilter()
        is TokenAuthConfiguration -> createTokenAuthFilter(config)
    }

private fun createDummyAuthFilter() =
    object : AuthFilterBuilder<String, User, DummyAuthFilter>() { override fun newInstance() = DummyAuthFilter() }
        .setAuthenticator { id -> Optional.of(User(id)) }
        .buildAuthFilter()

private fun createTokenAuthFilter(config: TokenAuthConfiguration): AuthFilter<String, User> {
    val jwtVerifier = JwtVerifier(config.base64EncodedKey, Clock.systemUTC())

    return OAuthCredentialAuthFilter.Builder<User>()
        .setAuthenticator({ credentials ->
            val user = jwtVerifier.verify(credentials)
            if (user != null) {
                Optional.of(User(user))
            } else {
                Optional.empty()
            }
        })
        .setPrefix("Bearer")
        .buildAuthFilter()
}
