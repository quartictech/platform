package io.quartic.howl.storage

import io.quartic.howl.storage.S3Storage.Config
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.ws.rs.core.MediaType

class S3StorageShould {
    @Test
    fun get_data_that_was_put() {
        val coords = StorageCoords("foo", UUID.randomUUID().toString(), "hello.txt")
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
        val coords = StorageCoords("foo", UUID.randomUUID().toString(), "hello.txt")

        assertThat(storage.getData(coords, null), nullValue())
    }

    fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
            = this.bufferedReader(charset).use { it.readText() }

    companion object {
        private val storage = S3Storage(Config(
                "eu-west-1",
                "test-howl",
                "arn:aws:iam::555071496850:role/Test-Bucket-Accessor",
                "696969"
        ))
    }
}