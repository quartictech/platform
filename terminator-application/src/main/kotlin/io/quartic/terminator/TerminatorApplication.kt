package io.quartic.terminator

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.common.websocket.serverEndpointConfig
import javax.websocket.server.ServerEndpointConfig

class TerminatorApplication : ApplicationBase<TerminatorConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<TerminatorConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: TerminatorConfiguration, environment: Environment) {
        val catalogueWatcher = CatalogueWatcher(WebsocketListener.Factory(
                configuration.catalogueWatchUrl!!,
                WebsocketClientSessionFactory(javaClass)
        ))
        val terminator = TerminatorResource(catalogueWatcher)
        websocketBundle.addEndpoint(serverEndpointConfig("/ws", WebsocketEndpoint(terminator.featureCollections)))

        environment.jersey().register(terminator)

        catalogueWatcher.start()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            TerminatorApplication().run(*args)
        }
    }
}
