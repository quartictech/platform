package io.quartic.orf

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenGenerator
import java.time.Duration

class OrfApplication : ApplicationBase<OrfConfiguration>() {
    override fun runApplication(configuration: OrfConfiguration, environment: Environment) {
        environment.jersey().register(OrfResource(TokenGenerator(
            (configuration.auth as TokenAuthConfiguration).base64EncodedKey,
            Duration.ofMinutes(configuration.tokenTimeToLiveMinutes.toLong())
        )))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = OrfApplication().run(*args)
    }
}
