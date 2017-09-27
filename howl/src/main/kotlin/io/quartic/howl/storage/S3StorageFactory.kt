package io.quartic.howl.storage

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.regions.AwsRegionProvider
import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import java.io.InputStream

class S3StorageFactory(
    private val secretsCodec: SecretsCodec,
    credsProvider: AWSCredentialsProvider = DefaultAWSCredentialsProviderChain.getInstance(),
    regionProvider: AwsRegionProvider = DefaultAwsRegionProviderChain()
) {
    data class Config(
        val region: String,
        val bucketEncrypted: EncryptedSecret,
        val roleArnEncrypted: EncryptedSecret,
        val externalIdEncrypted: EncryptedSecret
    ) : StorageConfig

    private val stsClient = AWSSecurityTokenServiceClientBuilder
        .standard()
        .withRegion(regionProvider.region)
        .withCredentials(credsProvider)
        .build()

    init {
        stsClient.sessionToken  // Validate the creds by doing something with the client
    }

    fun create(config: Config) = object : Storage {
        private val s3 by lazy {
            val stsProvider = STSAssumeRoleSessionCredentialsProvider.Builder(config.roleArnEncrypted.decrypt().veryUnsafe, ROLE_SESSION_NAME)
                .withExternalId(config.externalIdEncrypted.decrypt().veryUnsafe)
                .withStsClient(stsClient)
                .build()

            AmazonS3ClientBuilder
                .standard()
                .withCredentials(stsProvider)
                .withRegion(config.region)
                .build()
        }

        private val bucket = config.bucketEncrypted.decrypt()

        override fun getData(coords: StorageCoords): Storage.StorageResult? = wrapS3Exception {
            val s3Object = s3.getObject(bucket.veryUnsafe, coords.bucketKey)
            StorageResult(
                storageMetadata(s3Object.objectMetadata),
                s3Object.objectContent
            )
        }

        override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapS3Exception {
            s3.getObjectMetadata(bucket.veryUnsafe, coords.bucketKey)
                .let { storageMetadata(it) }
        }

        private fun storageMetadata(objectMetadata: ObjectMetadata) =
            StorageMetadata(
                objectMetadata.lastModified.toInstant(),
                objectMetadata.contentType,
                objectMetadata.contentLength
            )

        override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean {
            inputStream.use { s ->
                val metadata = ObjectMetadata()
                if (contentLength != null && contentLength > 0) {
                    metadata.contentLength = contentLength.toLong()
                }
                metadata.contentType = contentType
                s3.putObject(bucket.veryUnsafe, coords.bucketKey, s, metadata)
            }
            return true
        }

        private fun <T> wrapS3Exception(block: () -> T): T? = try {
           block()
        } catch (e: AmazonS3Exception) {
            if (e.errorCode != "NoSuchKey" && e.statusCode != 404) {
                throw e
            } else {
                null
            }
        }


    }

    private fun EncryptedSecret.decrypt() = secretsCodec.decrypt(this)

    companion object {
        private val ROLE_SESSION_NAME = "quartic"
    }
}
