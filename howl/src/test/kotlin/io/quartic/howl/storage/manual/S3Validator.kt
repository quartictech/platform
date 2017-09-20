package io.quartic.howl.storage.manual

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.howl.storage.S3StorageFactory
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.S3StorageFactory.Config
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    val storage = S3StorageFactory(SecretsCodec(DEV_MASTER_KEY_BASE64)).create(Config(
        region = args[0],
        bucketEncrypted = EncryptedSecret(args[1]),
        roleArnEncrypted = EncryptedSecret(args[2]),
        externalIdEncrypted = EncryptedSecret(args[3])
    ))

    val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
    val data = "Hello world!"

    storage.putData(coords, data.length, MediaType.TEXT_PLAIN, data.byteInputStream())
    storage.getData(coords, null).use {
        it!!
        assertThat(it.metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(it.inputStream.readTextAndClose(), equalTo(data))
    }
}

private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
        = this.bufferedReader(charset).use { it.readText() }
