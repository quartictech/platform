package io.quartic.home

import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.CatalogueService
import io.quartic.common.application.ApplicationBase
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.client.ClientBuilder.Companion.userAgentFor
import io.quartic.common.healthcheck.PingPongHealthCheck
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.home.resource.AuthResource
import io.quartic.home.resource.GraphqlResource
import io.quartic.home.resource.HomeResource
import io.quartic.home.resource.UserResource
import io.quartic.howl.api.HowlClient
import io.quartic.registry.api.RegistryServiceClient
import java.time.Duration

class HomeApplication : ApplicationBase<HomeConfiguration>() {

    public override fun runApplication(configuration: HomeConfiguration, environment: Environment) {
        val howl = HowlClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogue = clientBuilder.feign<CatalogueService>(configuration.catalogueUrl)
        val registry = clientBuilder.retrofit<RegistryServiceClient>(configuration.registryUrl)
        val evalQuery = clientBuilder.retrofit<EvalQueryServiceClient>(configuration.evalUrl)
        val evalTrigger = clientBuilder.retrofit<EvalTriggerServiceClient>(configuration.evalUrl)

        val tokenGenerator = TokenGenerator(
            configuration.auth as TokenAuthConfiguration,
            configuration.secretsCodec,
            Duration.ofSeconds(configuration.cookies.maxAgeSeconds.toLong())
        )

        with (environment.jersey()) {
            register(GraphqlResource(evalQuery))
            register(UserResource(clientBuilder.feign(configuration.github.apiRoot)))
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
                clientBuilder
            ))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(clientBuilder, configuration.catalogueUrl))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HomeApplication().run(*args)
    }
}
