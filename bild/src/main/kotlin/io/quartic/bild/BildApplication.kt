package io.quartic.bild

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.resource.BildResource
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger

class BildApplication : ApplicationBase<BildConfiguration>() {
    private val LOG by logger()

    public override fun initializeApplication(bootstrap: Bootstrap<BildConfiguration>) {
    }

    override fun runApplication(configuration: BildConfiguration, environment: Environment) {

        with (environment.jersey()) {
            register(BildResource(configuration.template))
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
