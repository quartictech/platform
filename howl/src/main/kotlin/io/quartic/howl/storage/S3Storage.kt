package io.quartic.howl.storage

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.regions.AwsRegionProvider
import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import java.io.InputStream

// http://docs.aws.amazon.com/AmazonS3/latest/dev/Introduction.html#ConsistencyModel is relevant here
class S3Storage(
    private val bucket: UnsafeSecret,
    s3Supplier: () -> AmazonS3
) : Storage {

    data class Config(
        val region: String,
        val bucketEncrypted: EncryptedSecret,
        val roleArnEncrypted: EncryptedSecret,
        val externalIdEncrypted: EncryptedSecret
    ) : StorageConfig

    class Factory(
        private val secretsCodec: SecretsCodec,
        credsProvider: AWSCredentialsProvider = DefaultAWSCredentialsProviderChain.getInstance(),
        regionProvider: AwsRegionProvider = DefaultAwsRegionProviderChain()
    ) {
        private val stsClient = AWSSecurityTokenServiceClientBuilder
            .standard()
            .withRegion(regionProvider.region)
            .withCredentials(credsProvider)
            .build()

        init {
            stsClient.sessionToken  // Validate the creds by doing something with the client
        }

        fun create(config: Config) = S3Storage(config.bucketEncrypted.decrypt()) {
            val stsProvider = STSAssumeRoleSessionCredentialsProvider
                .Builder(config.roleArnEncrypted.decrypt().veryUnsafe, ROLE_SESSION_NAME)
                .withExternalId(config.externalIdEncrypted.decrypt().veryUnsafe)
                .withStsClient(stsClient)
                .build()

            AmazonS3ClientBuilder
                .standard()
                .withCredentials(stsProvider)
                .withRegion(config.region)
                .build()
        }

        private fun EncryptedSecret.decrypt() = secretsCodec.decrypt(this)
    }

    private val s3 by lazy(s3Supplier)

    override fun getObject(coords: StorageCoords): StorageResult? = wrapS3Exception {
        val s3Object = s3.getObject(bucket.veryUnsafe, coords.backendKey)
        StorageResult(
            storageMetadata(s3Object.objectMetadata),
            s3Object.objectContent
        )
    }

    override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapS3Exception {
        storageMetadata(getRawMetadata(coords))
    }

    override fun putObject(contentLength: Int?, contentType: String?, inputStream: InputStream, coords: StorageCoords): String =
        inputStream.use { s ->
            val metadata = ObjectMetadata()
            if (contentLength != null && contentLength > 0) {
                metadata.contentLength = contentLength.toLong()
            }
            metadata.contentType = contentType
            s3.putObject(bucket.veryUnsafe, coords.backendKey, s, metadata).eTag
        }

    override fun copyObject(source: StorageCoords, dest: StorageCoords, oldETag: String?): String? = wrapS3Exception {
        val copyRequest = CopyObjectRequest(
            bucket.veryUnsafe,
            source.backendKey,
            bucket.veryUnsafe,
            dest.backendKey
        )

        if (oldETag != null) {
            copyRequest.withNonmatchingETagConstraint(oldETag)
        }

        s3.copyObject(copyRequest)?.eTag ?: oldETag
    }

    private fun getRawMetadata(coords: StorageCoords) = s3.getObjectMetadata(bucket.veryUnsafe, coords.backendKey)

    private fun storageMetadata(objectMetadata: ObjectMetadata) =
        StorageMetadata(
            objectMetadata.lastModified.toInstant(),
            objectMetadata.contentType,
            objectMetadata.contentLength,
            objectMetadata.eTag
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

    companion object {
        private val ROLE_SESSION_NAME = "quartic"
    }
}
