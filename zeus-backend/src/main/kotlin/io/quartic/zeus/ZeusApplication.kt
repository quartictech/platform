package io.quartic.zeus

import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.provider.ClasspathDataProvider
import io.quartic.zeus.provider.UrlDataProvider
import io.quartic.zeus.resource.DatasetResource

class ZeusApplication : ApplicationBase<ZeusConfiguration>() {
    private val LOG by logger()

    public override fun initializeApplication(bootstrap: Bootstrap<ZeusConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
    }

    override fun runApplication(configuration: ZeusConfiguration, environment: Environment) {
        environment.jersey().register(DatasetResource(createDatasetMap(configuration.datasets)))
    }

    private fun createDatasetMap(config: Map<DatasetName, DataProviderConfiguration>) = config
            .onEach { LOG.info("Registered data provider: ${it.key} -> ${it.value}") }
            .mapValues {
                val providerConfig = it.value
                when (providerConfig) {
                    is ClasspathDataProviderConfiguration -> ClasspathDataProvider(providerConfig)
                    is UrlDataProviderConfiguration -> UrlDataProvider(providerConfig)
                }
            }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ZeusApplication().run(*args)
    }
}