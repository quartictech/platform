package io.quartic.howl.s3

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import java.io.InputStream
import java.nio.charset.Charset
import javax.ws.rs.core.MediaType

class S3Validator(
        region: String,
        private val bucketName: String,
        roleArn: String,
        externalId: String
) {
    private val s3 by lazy {
        val provider = STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, ROLE_SESSION_NAME)
                .withExternalId(externalId)
                .build()
        AmazonS3ClientBuilder.standard().withCredentials(provider).withRegion(region).build()
    }


    fun validate() {
        val objectKey = "test/test.json"
        val content = """{ "message": "Hello world" }"""

        content.byteInputStream().use { s ->
            val metadata = ObjectMetadata()
            metadata.contentType = MediaType.APPLICATION_JSON
            s3.putObject(bucketName, objectKey, s, metadata)
        }

        val s3obj = s3.getObject(bucketName, objectKey)
        assertThat(s3obj.objectMetadata.contentType, equalTo(MediaType.APPLICATION_JSON))
        assertThat(s3obj.objectContent.readTextAndClose(), equalTo(content))
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
            = this.bufferedReader(charset).use { it.readText() }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val region = args[0]
            val bucketName = args[1]
            val roleArn = args[2]
            val externalId = args[3]

            S3Validator(region, bucketName, roleArn, externalId).validate()
        }

        val ROLE_SESSION_NAME = "quartic-test"
    }
}