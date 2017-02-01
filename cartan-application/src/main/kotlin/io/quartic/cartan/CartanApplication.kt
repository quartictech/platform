package io.quartic.cartan

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import javax.websocket.server.ServerEndpointConfig

class CartanApplication : ApplicationBase<CartanConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<CartanConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: CartanConfiguration, environment: Environment) {
        val catalogueWatcher = CatalogueWatcher(WebsocketListener.Factory(
                configuration.catalogueWatchUrl!!,
                WebsocketClientSessionFactory(javaClass)
        ))
        //websocketBundle.addEndpoint(serverEndpointConfig("/ws", WebsocketEndpoint(terminator.featureCollections)))

        //environment.jersey().register(terminator)

        catalogueWatcher.start()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CartanApplication().run(*args)
        }
    }
}
