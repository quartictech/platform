package io.quartic.howl

import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.serverEndpointConfig
import io.quartic.howl.storage.DiskStorageBackend
import io.quartic.howl.storage.GcsStorageBackend
import io.quartic.howl.storage.ObservableStorageBackend
import javax.websocket.server.ServerEndpointConfig

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<HowlConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        val backend = createBackend(configuration)
        environment.jersey().register(HowlResource(backend))
        websocketBundle.addEndpoint(serverEndpointConfig(
                "/changes/{namespace}/{objectName}",
                WebsocketEndpoint(backend.changes)
        ))
    }

    private fun createBackend(configuration: HowlConfiguration): ObservableStorageBackend {
        val delegate = if (configuration.localDisk) {
            DiskStorageBackend(DiskStorageBackend.Config())
        } else {
            GcsStorageBackend(configuration.gcs)
        }

        return ObservableStorageBackend(delegate)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HowlApplication().run(*args)
    }
}
