package io.quartic.zeus

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.provider.UrlDataProvider
import io.quartic.zeus.resource.DatasetResource
import io.quartic.zeus.resource.SessionInfoResource

class ZeusApplication : ApplicationBase<ZeusConfiguration>() {
    private val LOG by logger()

    override fun runApplication(configuration: ZeusConfiguration, environment: Environment) {
        with (environment.jersey()) {
            register(SessionInfoResource())
            register(DatasetResource(createDatasetMap(configuration.datasets)))
        }
    }

    private fun createDatasetMap(config: Map<DatasetName, DataProviderConfiguration>) = config
            .onEach { LOG.info("Registered data provider: ${it.key} -> ${it.value}") }
            .mapValues { UrlDataProvider(it.value) }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ZeusApplication().run(*args)
    }
}
