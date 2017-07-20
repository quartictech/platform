package io.quartic.glisten

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase

class GlistenApplication : ApplicationBase<GlistenConfiguration>() {
    override fun runApplication(configuration: GlistenConfiguration, environment: Environment) {
        environment.jersey().register(GithubResource(configuration.registrations, {}))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = GlistenApplication().run(*args)
    }
}
