package io.quartic.catalogue

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.catalogue.database.Database
import io.quartic.common.application.ApplicationBase
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.websocket.serverEndpointConfig
import javax.websocket.server.ServerEndpointConfig

class CatalogueApplication : ApplicationBase<CatalogueConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOfNulls<ServerEndpointConfig>(0))

    public override fun initializeApplication(bootstrap: Bootstrap<CatalogueConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: CatalogueConfiguration, environment: Environment) {
        val database = database(configuration, environment)
        val catalogue = CatalogueResource(database)
        environment.jersey().register(catalogue)
        websocketBundle.addEndpoint(serverEndpointConfig("/api/datasets/watch", catalogue))
    }

    private fun database(config: CatalogueConfiguration, environment: Environment) =
        DatabaseBuilder(
            javaClass,
            config.database,
            environment,
            config.secretsCodec
        ).dao<Database>()

    companion object {
        @JvmStatic fun main(args: Array<String>) = CatalogueApplication().run(*args)
    }
}
