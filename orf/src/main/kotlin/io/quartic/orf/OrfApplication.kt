package io.quartic.orf

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.uid.randomGenerator
import io.quartic.orf.model.JwtId
import java.time.Clock
import java.time.Duration

class OrfApplication : ApplicationBase<OrfConfiguration>() {
    override fun runApplication(configuration: OrfConfiguration, environment: Environment) {
        environment.jersey().register(OrfResource(
            configuration.base64EncodedKey!!,
            Duration.ofMinutes(configuration.tokenTimeToLiveMinutes.toLong()),
            Clock.systemUTC(),
            randomGenerator(::JwtId)
        ))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = OrfApplication().run(*args)
    }
}
