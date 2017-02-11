package io.quartic.rain

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.howl.api.HowlClient

import javax.websocket.server.ServerEndpointConfig
import io.quartic.common.client.userAgentFor

import io.quartic.common.websocket.serverEndpointConfig

class RainApplication : ApplicationBase<RainConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<RainConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: RainConfiguration, environment: Environment) {
        val websocketFactory = WebsocketClientSessionFactory(javaClass)
        val howlClient = HowlClient(userAgentFor(RainApplication::class.java), configuration.howlUrl)
        val websocketEndpoint = WebsocketEndpoint(configuration.howlWatchUrl!!, websocketFactory, howlClient)
        websocketBundle.addEndpoint(serverEndpointConfig("/ws/{namespace}/{objectName}", websocketEndpoint))
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            RainApplication().run(*args)
        }
    }
}
