package io.quartic.howl

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.serverEndpointConfig
import io.quartic.howl.HowlConfiguration.S3Configuration
import io.quartic.howl.storage.GcsStorageFactory
import io.quartic.howl.storage.ObservableStorage
import io.quartic.howl.storage.RoutingStorage
import io.quartic.howl.storage.S3StorageFactory
import javax.websocket.server.ServerEndpointConfig

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    private val websocketBundle = WebsocketBundle(*arrayOf<ServerEndpointConfig>())

    public override fun initializeApplication(bootstrap: Bootstrap<HowlConfiguration>) {
        bootstrap.addBundle(websocketBundle)
    }

    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        val storage = ObservableStorage(RoutingStorage(
            GcsStorageFactory(),
            S3StorageFactory(s3CredentialsProvider(configuration.s3)),
            configuration.namespaces
        ))
        environment.jersey().register(HowlResource(storage))
        websocketBundle.addEndpoint(serverEndpointConfig(
                "/changes/{namespace}/{objectName}",
                WebsocketEndpoint(storage.changes)
        ))
    }

    private fun s3CredentialsProvider(config: S3Configuration?) = if (config == null) {
        DefaultAWSCredentialsProviderChain()
    } else {
        AWSStaticCredentialsProvider(
            BasicAWSCredentials(
                config.accessKeyId,
                config.secretAccessKeyEncrypted.decrypt().veryUnsafe
            )
        )
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HowlApplication().run(*args)
    }
}
