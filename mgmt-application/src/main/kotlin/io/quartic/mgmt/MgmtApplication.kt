package io.quartic.mgmt

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.CatalogueService
import io.quartic.common.application.ApplicationBase
import io.quartic.common.client.client
import io.quartic.common.client.userAgentFor
import io.quartic.common.healthcheck.PingPongHealthCheck
import io.quartic.howl.api.HowlClient
import io.quartic.mgmt.auth.NoobAuthFilter
import io.quartic.mgmt.auth.User


class MgmtApplication : ApplicationBase<MgmtConfiguration>() {

    public override fun initializeApplication(bootstrap: Bootstrap<MgmtConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    public override fun runApplication(configuration: MgmtConfiguration, environment: Environment) {
        val howlService = HowlClient(userAgentFor(javaClass), configuration.howlUrl)
        val catalogueService = client(CatalogueService::class.java, javaClass, configuration.catalogueUrl!!)

        with (environment.jersey()) {
            register(AuthDynamicFeature(NoobAuthFilter.create()))
            register(AuthValueFactoryProvider.Binder(User::class.java))
            register(MgmtResource(catalogueService, howlService, configuration.defaultCatalogueNamespace!!))
        }
        environment.healthChecks().register("catalogue", PingPongHealthCheck(javaClass, configuration.catalogueUrl!!))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = MgmtApplication().run(*args)
    }
}
