package io.quartic.zeus

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.resource.DatasetResource

class ZeusApplication : ApplicationBase<ZeusConfiguration>() {
    public override fun initializeApplication(bootstrap: Bootstrap<ZeusConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    override fun runApplication(configuration: ZeusConfiguration, environment: Environment) {
        environment.jersey().register(DatasetResource(mapOf(
                DatasetName("assets") to ClasspathDataProvider("/assets.json")  // TODO: get rid of this long-term
        )))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ZeusApplication().run(*args)
    }
}