package io.quartic.glisten

import io.dropwizard.setup.Environment
import io.quartic.bild.api.BildTriggerService
import io.quartic.common.application.ApplicationBase
import io.quartic.common.client.client

class GlistenApplication : ApplicationBase<GlistenConfiguration>() {
    override fun runApplication(configuration: GlistenConfiguration, environment: Environment) {
        val trigger = client<BildTriggerService>(javaClass, configuration.bildUrl)

        environment.jersey().register(GithubResource(
            configuration.secretToken,
            trigger
        ))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = GlistenApplication().run(*args)
    }
}
