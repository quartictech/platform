package io.quartic.tracker.scribe

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.glisten.GithubResource

class GlistenApplication : ApplicationBase<GlistenConfiguration>() {
    override fun runApplication(configuration: GlistenConfiguration, environment: Environment) {
        environment.jersey().register(GithubResource())
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = GlistenApplication().run(*args)
    }
}
