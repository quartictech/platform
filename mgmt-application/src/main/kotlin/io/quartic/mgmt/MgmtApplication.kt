package io.quartic.mgmt

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.CatalogueService
import io.quartic.common.application.ApplicationBase
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.client.client
import io.quartic.common.client.userAgentFor
import io.quartic.common.healthcheck.PingPongHealthCheck
import io.quartic.howl.api.HowlClient
import io.quartic.mgmt.auth.NamespaceAuthoriser
import java.time.Duration


class MgmtApplication : ApplicationBase<MgmtConfiguration>() {

    public override fun initializeApplication(bootstrap: Bootstrap<MgmtConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    public override fun runApplication(configuration: MgmtConfiguration, environment: Environment) {
        val howlService = HowlClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogueService = client<CatalogueService>(javaClass, configuration.catalogueUrl)

        val tokenGenerator = TokenGenerator(
            (configuration.auth as TokenAuthConfiguration).base64EncodedKey,
            Duration.ofMinutes(configuration.tokenTimeToLiveMinutes.toLong())
        )

        with (environment.jersey()) {
            register(MgmtResource(catalogueService, howlService, NamespaceAuthoriser(emptyMap())))  // TODO
            register(AuthResource(configuration.github, configuration.cookies, tokenGenerator))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(javaClass, configuration.catalogueUrl))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = MgmtApplication().run(*args)
    }
}
