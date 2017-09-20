package io.quartic.howl.storage

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.storage.S3StorageFactory.Config
import io.quartic.howl.storage.StorageCoords.Managed
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.InputStream
import java.nio.charset.Charset
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.ws.rs.core.MediaType

class S3StorageFactoryShould {

    @Test
    fun get_data_that_was_put() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, data.length, MediaType.TEXT_PLAIN, data.byteInputStream())

        storage.getData(coords, null).use {
            it!!
            assertThat(it.metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
            assertThat(it.inputStream.readTextAndClose(), equalTo(data))
        }
    }

    @Test
    fun ignore_content_length_if_negative() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, -1, MediaType.TEXT_PLAIN, data.byteInputStream())
    }

    @Test
    fun return_null_if_key_not_found() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")

        assertThat(storage.getData(coords, null), nullValue())
    }

    @Test
    fun return_null_metadata_if_key_not_found() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")

        assertThat(storage.getMetadata(coords, null), nullValue())
    }

    @Test
    fun store_metadata() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, null, MediaType.TEXT_PLAIN, data.byteInputStream())
        val metadata = storage.getData(coords, null)!!.metadata
        assertThat(metadata.contentLength, equalTo(12L))
        assertThat(metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(metadata.lastModified, greaterThan(Instant.now().minus(5, ChronoUnit.MINUTES)))
        assertThat(metadata.lastModified, lessThan(Instant.now().plus(5, ChronoUnit.MINUTES)))
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
            = this.bufferedReader(charset).use { it.readText() }

    companion object {
        private val codec = SecretsCodec(DEV_MASTER_KEY_BASE64)

        private val storage = S3StorageFactory(codec)
            .create(Config(
                "eu-west-1",
                codec.encrypt(UnsafeSecret("test-howl")),
                codec.encrypt(UnsafeSecret("arn:aws:iam::555071496850:role/Test-Bucket-Accessor")),
                codec.encrypt(UnsafeSecret("696969"))
            ))
    }
}
