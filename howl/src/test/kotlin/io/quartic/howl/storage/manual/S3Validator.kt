package io.quartic.howl.storage.manual

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.howl.storage.S3Storage.Config
import io.quartic.howl.storage.S3Storage.Factory
import io.quartic.howl.storage.StorageCoords.Managed
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    val storage = Factory(SecretsCodec(DEV_MASTER_KEY_BASE64)).create(Config(
        region = args[0],
        bucketEncrypted = EncryptedSecret(args[1]),
        roleArnEncrypted = EncryptedSecret(args[2]),
        externalIdEncrypted = EncryptedSecret(args[3])
    ))

    val coords = Managed(UUID.randomUUID().toString(), "hello.txt")
    val data = "Hello world!"

    storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
    storage.getObject(coords).use {
        it!!
        assertThat(it.metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(it.inputStream.readTextAndClose(), equalTo(data))
    }
}

private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
        = this.bufferedReader(charset).use { it.readText() }
