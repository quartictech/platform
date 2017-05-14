package io.quartic.zeus

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase

class ZeusApplication : ApplicationBase<ZeusConfiguration>() {
    public override fun initializeApplication(bootstrap: Bootstrap<ZeusConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    override fun runApplication(configuration: ZeusConfiguration, environment: Environment) {
        // TODO
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ZeusApplication().run(*args)
    }
}