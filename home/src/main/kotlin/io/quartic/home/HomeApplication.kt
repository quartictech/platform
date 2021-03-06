package io.quartic.home

import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.common.application.ApplicationBase
import io.quartic.common.auth.frontend.FrontendAuthStrategy
import io.quartic.common.auth.frontend.FrontendTokenGenerator
import io.quartic.common.client.ClientBuilder.Companion.userAgentFor
import io.quartic.common.healthcheck.PingPongHealthCheck
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.github.GitHubClient
import io.quartic.github.GitHubOAuthClient
import io.quartic.home.howl.HowlStreamingClient
import io.quartic.home.resource.AuthResource
import io.quartic.home.resource.GraphQLResource
import io.quartic.home.resource.HomeResource
import io.quartic.registry.api.RegistryServiceClient
import java.time.Duration

class HomeApplication : ApplicationBase<HomeConfiguration>() {

    public override fun runApplication(configuration: HomeConfiguration, environment: Environment) {
        val howl = HowlStreamingClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogue = clientBuilder.retrofit<CatalogueClient>(configuration.catalogueUrl)
        val registry = clientBuilder.retrofit<RegistryServiceClient>(configuration.registryUrl)
        val evalQuery = clientBuilder.retrofit<EvalQueryServiceClient>(configuration.evalUrl)
        val evalTrigger = clientBuilder.retrofit<EvalTriggerServiceClient>(configuration.evalUrl)
        val githubOAuth = clientBuilder.retrofit<GitHubOAuthClient>(configuration.github.oauthApiRoot)
        val github = clientBuilder.retrofit<GitHubClient>(configuration.github.apiRoot)

        val tokenGenerator = FrontendTokenGenerator(
            signingKeyBase64(configuration),
            Duration.ofSeconds(configuration.cookies.maxAgeSeconds.toLong())
        )

        with (environment.jersey()) {
            register(GraphQLResource(evalQuery, github))
            register(HomeResource(
                catalogue,
                howl,
                evalQuery,
                evalTrigger,
                registry
            ))
            register(AuthResource(
                configuration.github,
                configuration.cookies,
                configuration.secretsCodec,
                tokenGenerator,
                registry,
                githubOAuth,
                github
            ))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(clientBuilder, configuration.catalogueUrl))
    }

    override fun authStrategy(configuration: HomeConfiguration) = FrontendAuthStrategy(signingKeyBase64(configuration))

    private fun signingKeyBase64(configuration: HomeConfiguration) = configuration.cookies.signingKeyEncryptedBase64.decrypt()

    companion object {
        @JvmStatic fun main(args: Array<String>) = HomeApplication().run(*args)
    }
}
