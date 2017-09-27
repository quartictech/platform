package io.quartic.howl.storage

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.regions.AwsRegionProvider
import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import java.io.InputStream

// http://docs.aws.amazon.com/AmazonS3/latest/dev/Introduction.html#ConsistencyModel is relevant here
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

        override fun getObject(coords: StorageCoords): StorageResult? = wrapS3Exception {
            val s3Object = s3.getObject(bucket.veryUnsafe, coords.backendKey)
            StorageResult(
                storageMetadata(s3Object.objectMetadata),
                s3Object.objectContent
            )
        }

        override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapS3Exception {
            storageMetadata(s3.getObjectMetadata(bucket.veryUnsafe, coords.backendKey))
        }

        // TODO - need to catch exceptions here
        override fun putObject(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean {
            inputStream.use { s ->
                val metadata = ObjectMetadata()
                if (contentLength != null && contentLength > 0) {
                    metadata.contentLength = contentLength.toLong()
                }
                metadata.contentType = contentType
                s3.putObject(bucket.veryUnsafe, coords.backendKey, s, metadata)
            }
            return true
        }

        override fun copyObject(source: StorageCoords, dest: StorageCoords): StorageMetadata? = wrapS3Exception {
            // CopyObjectResult doesn't have all the required metadata, so we have to do another API request here :(
            // We can't query the destination metadata, because of eventual consistency.  So we query the source metadata
            // instead.  To mitigate the potential race condition, we enforce an ETag constraint, and keep looping
            // until it's met.
            var result: StorageMetadata? = null
            while (result == null) {
                result = attemptSafeCopy(source, dest)
            }
            result
        }

        private fun attemptSafeCopy(source: StorageCoords, dest: StorageCoords): StorageMetadata? {
            val sourceMetadata = s3.getObjectMetadata(bucket.veryUnsafe, source.backendKey)

            val copyRequest = CopyObjectRequest(
                bucket.veryUnsafe,
                source.backendKey,
                bucket.veryUnsafe,
                dest.backendKey
            ).withMatchingETagConstraint(sourceMetadata.eTag)

            return if (s3.copyObject(copyRequest) != null) {
                storageMetadata(sourceMetadata)
            } else {
                null
            }
        }

        private fun storageMetadata(objectMetadata: ObjectMetadata) =
            StorageMetadata(
                objectMetadata.lastModified.toInstant(),
                objectMetadata.contentType,
                objectMetadata.contentLength
            )

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
