package io.quartic.howl.s3

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.nio.charset.Charset
import javax.ws.rs.core.MediaType

class S3Stuff {
    @Test
    fun does_bucket_exist() {
        assertTrue(s3.doesBucketExist(BUCKET_NAME))
    }

    @Test
    fun write_then_read_from_bucket() {
        val content = """{ "message": "Hello world" }"""

        content.byteInputStream().use { s ->
            val metadata = ObjectMetadata()
            metadata.contentType = MediaType.APPLICATION_JSON
            s3.putObject(BUCKET_NAME, "magic/stuff", s, metadata)
        }

        val s3obj = s3.getObject(BUCKET_NAME, "magic/stuff")
        assertThat(s3obj.objectMetadata.contentType, equalTo(MediaType.APPLICATION_JSON))
        assertThat(s3obj.objectContent.readTextAndClose(), equalTo(content))
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
            = this.bufferedReader(charset).use { it.readText() }

    companion object {
        private val s3 = AmazonS3ClientBuilder.defaultClient()
        val BUCKET_NAME = "howl-test"
    }
}