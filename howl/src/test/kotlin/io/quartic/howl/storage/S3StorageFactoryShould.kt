package io.quartic.howl.storage

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.storage.S3StorageFactory.Config
import io.quartic.howl.storage.StorageCoords.Managed
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.InputStream
import java.nio.charset.Charset
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
            assertThat(it.contentType, equalTo(MediaType.TEXT_PLAIN))
            assertThat(it.inputStream.readTextAndClose(), equalTo(data))
        }
    }

    @Test
    fun return_null_if_key_not_found() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")

        assertThat(storage.getData(coords, null), nullValue())
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
