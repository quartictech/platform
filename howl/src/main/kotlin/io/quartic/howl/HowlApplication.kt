package io.quartic.howl

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.AwsRegionProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.websockets.WebsocketBundle
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.serverEndpointConfig
import io.quartic.howl.HowlConfiguration.AwsConfiguration
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
            s3StorageFactory(configuration.aws),
            configuration.namespaces
        ))
        environment.jersey().register(HowlResource(storage))
        websocketBundle.addEndpoint(serverEndpointConfig(
                "/changes/{namespace}/{objectName}",
                WebsocketEndpoint(storage.changes)
        ))
    }

    private fun s3StorageFactory(config: AwsConfiguration?) = if (config == null) {
        S3StorageFactory(secretsCodec)
    } else {
        S3StorageFactory(
            secretsCodec,
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    config.accessKeyId,
                    config.secretAccessKeyEncrypted.decrypt().veryUnsafe
                )
            ),
            object : AwsRegionProvider() {
                override fun getRegion() = config.region
            }
        )

    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = HowlApplication().run(*args)
    }
}
