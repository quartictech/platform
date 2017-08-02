package io.quartic.bild

import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.bild.resource.BildResource
import io.quartic.common.application.ApplicationBase
import io.quartic.common.serdes.OBJECT_MAPPER

class BildApplication : ApplicationBase<BildConfiguration>() {
    public override fun initializeApplication(bootstrap: Bootstrap<BildConfiguration>) {
    }

    override fun runApplication(configuration: BildConfiguration, environment: Environment) {

        with (environment.jersey()) {
            register(BildResource(
                mapOf("magnolia" to OBJECT_MAPPER.readValue<Any>(javaClass.getResourceAsStream("/pipeline.json")))
            ))
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
