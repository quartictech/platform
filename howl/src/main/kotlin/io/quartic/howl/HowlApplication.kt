package io.quartic.howl

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.AwsRegionProvider
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.howl.HowlConfiguration.AwsConfiguration
import io.quartic.howl.storage.GcsStorageFactory
import io.quartic.howl.storage.S3StorageFactory

class HowlApplication : ApplicationBase<HowlConfiguration>() {
    public override fun runApplication(configuration: HowlConfiguration, environment: Environment) {
        environment.jersey().register(HowlResource(storageFactory(configuration)))
    }

    private fun storageFactory(configuration: HowlConfiguration) = StorageFactory(
        GcsStorageFactory(),
        s3StorageFactory(configuration.aws),
        configuration.namespaces
    )

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
