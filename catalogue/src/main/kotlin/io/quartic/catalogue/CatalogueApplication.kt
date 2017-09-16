package io.quartic.catalogue

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.migration.CloudPathMigration
import io.quartic.common.application.ApplicationBase
import io.quartic.common.uid.randomGenerator
import io.quartic.common.websocket.serverEndpointConfig
import java.time.Clock
import javax.websocket.server.ServerEndpointConfig

class CatalogueApplication : ApplicationBase<CatalogueConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOfNulls<ServerEndpointConfig>(0))
    private val didGenerator = randomGenerator { uid: String -> DatasetId(uid) }

    public override fun initializeApplication(bootstrap: Bootstrap<CatalogueConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: CatalogueConfiguration, environment: Environment) {
        val storageBackend = configuration.backend.build()

        CloudPathMigration().migrate(storageBackend)

        val catalogue = CatalogueResource(storageBackend, didGenerator, Clock.systemUTC())
        environment.healthChecks().register("storageBackend", StorageBackendHealthCheck(storageBackend))
        environment.jersey().register(catalogue)
        websocketBundle.addEndpoint(serverEndpointConfig("/api/datasets/watch", catalogue))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = CatalogueApplication().run(*args)
    }
}
