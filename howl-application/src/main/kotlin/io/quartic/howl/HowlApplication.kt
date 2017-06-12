package io.quartic.howl

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.serverEndpointConfig
import io.quartic.howl.storage.ObservableStorageBackend
import javax.websocket.server.ServerEndpointConfig

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<HowlConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        val observableStorageBackend = ObservableStorageBackend(configuration.storage.build())
        environment.jersey().register(HowlResource(observableStorageBackend))
        websocketBundle.addEndpoint(serverEndpointConfig(
                "/changes/{namespace}/{objectName}",
                WebsocketEndpoint(observableStorageBackend.changes)
        ))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HowlApplication().run(*args)
    }
}
