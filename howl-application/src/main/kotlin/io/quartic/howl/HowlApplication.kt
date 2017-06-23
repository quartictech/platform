package io.quartic.howl

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.serverEndpointConfig
import io.quartic.howl.storage.GcsStorage
import io.quartic.howl.storage.LocalStorage
import io.quartic.howl.storage.ObservableStorage
import javax.websocket.server.ServerEndpointConfig

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<HowlConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        val storage = createStorage(configuration)
        environment.jersey().register(HowlResource(storage))
        websocketBundle.addEndpoint(serverEndpointConfig(
                "/changes/{namespace}/{objectName}",
                WebsocketEndpoint(storage.changes)
        ))
    }

    private fun createStorage(configuration: HowlConfiguration): ObservableStorage {
        // TODO - make this regular
        val config = configuration.namespaces.first()
        val delegate = when (config) {
            is LocalStorage.Config -> LocalStorage(config)
            is GcsStorage.Config -> GcsStorage(config)
            else -> throw RuntimeException("Unrecognised storage type '${config.javaClass}'")
        }
        return ObservableStorage(delegate)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HowlApplication().run(*args)
    }
}
