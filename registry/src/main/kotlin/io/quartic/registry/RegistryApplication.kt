package io.quartic.registry

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase

class RegistryApplication : ApplicationBase<RegistryConfiguration>() {
    override fun runApplication(configuration: RegistryConfiguration, environment: Environment) {
        environment.jersey().register(RegistryResource(configuration.customers))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = RegistryApplication().run(*args)
    }
}
