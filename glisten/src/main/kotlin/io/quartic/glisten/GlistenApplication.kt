package io.quartic.glisten

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase

// TODO - this app should arguably be even simpler and just ram stuff into a PubSub queue, in order to maximise uptime
class GlistenApplication : ApplicationBase<GlistenConfiguration>() {
    override fun runApplication(configuration: GlistenConfiguration, environment: Environment) {
        // TODO - wire through notify param to Bild client
        environment.jersey().register(GithubResource(
            configuration.registrations,
            configuration.secretToken,
            {}
        ))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = GlistenApplication().run(*args)
    }
}
