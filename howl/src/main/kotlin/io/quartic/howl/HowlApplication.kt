package io.quartic.howl

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.AwsRegionProvider
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.howl.HowlConfiguration.AwsConfiguration
import io.quartic.howl.storage.GcsStorage
import io.quartic.howl.storage.S3Storage
import io.quartic.howl.storage.StorageFactory

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        environment.jersey().register(HowlResource(storageFactory(configuration)))
    }

    private fun storageFactory(configuration: HowlConfiguration) = StorageFactory(
        GcsStorage.Factory(),
        s3StorageFactory(configuration.aws),
        configuration.namespaces
    )

    private fun s3StorageFactory(config: AwsConfiguration?) = if (config == null) {
        S3Storage.Factory(secretsCodec)
    } else {
        S3Storage.Factory(
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
