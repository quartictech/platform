package io.quartic.howl.storage

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import io.quartic.howl.storage.Storage.PutResult
import java.io.InputStream

class S3Storage(private val config: Config) : Storage {
    data class Config(
            val region: String,
            val bucket: String,
            val roleArn: String,
            val externalId: String
    ) : StorageConfig

    private val s3 by lazy {
        val provider = STSAssumeRoleSessionCredentialsProvider.Builder(config.roleArn, ROLE_SESSION_NAME)
                .withExternalId(config.externalId)
                .build()
        AmazonS3ClientBuilder.standard().withCredentials(provider).withRegion(config.region).build()
    }

    override fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType? {
        try {
            val s3obj = s3.getObject(config.bucket, coords.objectKey)
            return InputStreamWithContentType(s3obj.objectMetadata.contentType, s3obj.objectContent)
        } catch (e: AmazonS3Exception) {
            if (e.errorCode != "NoSuchKey") {
                throw e
            } else {
                return null
            }
        }
    }

    override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): PutResult? {
        inputStream.use { s ->
            val metadata = ObjectMetadata()
            metadata.contentType = contentType
            s3.putObject(config.bucket, coords.objectKey, s, metadata)
        }
        return PutResult(null)  // TODO - no versioning for now
    }

    private val StorageCoords.objectKey get() = "$identityNamespace/$objectName"

    companion object {
        private val ROLE_SESSION_NAME = "quartic"
    }
}