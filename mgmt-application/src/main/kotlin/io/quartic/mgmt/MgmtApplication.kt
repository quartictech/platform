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
import io.quartic.mgmt.bild.BildService
import io.quartic.registry.api.RegistryService
import java.time.Duration

class MgmtApplication : ApplicationBase<MgmtConfiguration>() {

    public override fun initializeApplication(bootstrap: Bootstrap<MgmtConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    public override fun runApplication(configuration: MgmtConfiguration, environment: Environment) {
        val howl = HowlClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogue = client<CatalogueService>(javaClass, configuration.catalogueUrl)
        val registry = client<RegistryService>(javaClass, configuration.registryUrl)

        val tokenGenerator = TokenGenerator(
            (configuration.auth as TokenAuthConfiguration).base64EncodedKey,
            Duration.ofMinutes(configuration.tokenTimeToLiveMinutes.toLong())
        )

        with (environment.jersey()) {
            register(MgmtResource(
                catalogue,
                howl,
                FakeBild(), // TODO
                NamespaceAuthoriser(configuration.authorisedNamespaces)
            ))
            register(AuthResource(configuration.github, configuration.cookies, tokenGenerator, registry))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(javaClass, configuration.catalogueUrl))
    }

    private class FakeBild : BildService {
        override fun getDag(customerId: String) = mapOf("a" to customerId)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = MgmtApplication().run(*args)
    }
}
