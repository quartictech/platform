package io.quartic.tracker.scribe

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.tracker.ScribeConfiguration

class ScribeApplication : ApplicationBase<ScribeConfiguration>() {
    override fun runApplication(configuration: ScribeConfiguration, environment: Environment) {
        throw UnsupportedOperationException("not implemented")
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ScribeApplication().run(*args)
    }
}