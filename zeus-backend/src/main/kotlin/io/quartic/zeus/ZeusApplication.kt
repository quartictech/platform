package io.quartic.zeus

import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import io.quartic.zeus.resource.DatasetResource

class ZeusApplication : ApplicationBase<ZeusConfiguration>() {
    public override fun initializeApplication(bootstrap: Bootstrap<ZeusConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    override fun runApplication(configuration: ZeusConfiguration, environment: Environment) {
        environment.jersey().register(DatasetResource(mapOf(
                DatasetName("assets") to assetProvider()
        )))
    }

    private fun assetProvider() = object : DataProvider {
        override val data by lazy {
            OBJECT_MAPPER.readValue<Map<ItemId, Map<String, Any>>>(javaClass.getResourceAsStream("/assets.json"))
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ZeusApplication().run(*args)
    }
}