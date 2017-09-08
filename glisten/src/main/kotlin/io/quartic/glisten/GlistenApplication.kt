package io.quartic.glisten

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.api.EvalTriggerService

class GlistenApplication : ApplicationBase<GlistenConfiguration>() {
    override fun runApplication(configuration: GlistenConfiguration, environment: Environment) {
        val trigger = clientBuilder.feign<EvalTriggerService>(configuration.evalUrl)

        environment.jersey().register(GithubResource(
            configuration.webhookSecretEncrypted.decrypt(),
            trigger
        ))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = GlistenApplication().run(*args)
    }
}
