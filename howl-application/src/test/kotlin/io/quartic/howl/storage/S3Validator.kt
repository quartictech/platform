package io.quartic.howl.storage

import com.amazonaws.regions.Regions
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    val storage = S3Storage(S3Storage.Config(
            region = Regions.fromName(args[0]),
            bucket = args[1],
            roleArn = args[2],
            externalId = args[3]
    ))

    val coords = StorageCoords("foo", UUID.randomUUID().toString(), "hello.txt")
    val data = "Hello world!"

    storage.putData(coords, MediaType.TEXT_PLAIN, data.byteInputStream())
    storage.getData(coords, null).use {
        it!!
        assertThat(it.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(it.inputStream.readTextAndClose(), equalTo(data))
    }
}

private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
        = this.bufferedReader(charset).use { it.readText() }
