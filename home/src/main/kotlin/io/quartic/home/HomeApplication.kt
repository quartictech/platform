package io.quartic.home

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.CatalogueService
import io.quartic.common.application.ApplicationBase
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.client.client
import io.quartic.common.client.retrofitClient
import io.quartic.common.client.userAgentFor
import io.quartic.common.healthcheck.PingPongHealthCheck
import io.quartic.home.resource.AuthResource
import io.quartic.home.resource.HomeResource
import io.quartic.home.resource.UserResource
import io.quartic.howl.api.HowlClient
import io.quartic.qube.api.QubeQueryService
import io.quartic.registry.api.RegistryServiceClient
import java.time.Duration

class HomeApplication : ApplicationBase<HomeConfiguration>() {

    public override fun initializeApplication(bootstrap: Bootstrap<HomeConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    public override fun runApplication(configuration: HomeConfiguration, environment: Environment) {
        val howl = HowlClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogue = client<CatalogueService>(javaClass, configuration.catalogueUrl)
        val registry = retrofitClient<RegistryServiceClient>(javaClass, configuration.registryUrl)
        val qube = client<QubeQueryService>(javaClass, configuration.qubeUrl)

        val tokenGenerator = TokenGenerator(
            configuration.auth as TokenAuthConfiguration,
            configuration.secretsCodec,
            Duration.ofMinutes(configuration.tokenTimeToLiveMinutes.toLong())
        )

        with (environment.jersey()) {
            register(UserResource(configuration.github))
            register(HomeResource(
                catalogue,
                howl,
                qube,
                registry
            ))
            register(AuthResource(
                configuration.github,
                configuration.cookies,
                configuration.secretsCodec,
                tokenGenerator,
                registry
            ))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(javaClass, configuration.catalogueUrl))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HomeApplication().run(*args)
    }
}
