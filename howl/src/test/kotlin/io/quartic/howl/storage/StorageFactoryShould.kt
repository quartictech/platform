package io.quartic.howl.storage

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.storage.StorageCoords.Managed
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.ws.rs.core.MediaType

@RunWith(Parameterized::class)
class StorageFactoryShould {
    @Parameterized.Parameter
    lateinit var storageFactory: (File) -> Storage

    // Needed to keep junit happy
    @Parameterized.Parameter(1)
    lateinit var name: String

    @Rule
    @JvmField
    val folder = TemporaryFolder()
    val storage by lazy { storageFactory(folder.root) }

    @Test
    fun get_data_that_was_put() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, data.length, MediaType.TEXT_PLAIN, data.byteInputStream())

        storage.getData(coords).use {
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

        assertThat(storage.getData(coords), nullValue())
    }

    @Test
    fun return_null_metadata_if_key_not_found() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")

        assertThat(storage.getMetadata(coords), nullValue())
    }

    @Test
    fun store_metadata() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, null, MediaType.TEXT_PLAIN, data.byteInputStream())
        val metadata = storage.getData(coords)!!.metadata
        assertThat(metadata.contentLength, equalTo(12L))
        assertThat(metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(metadata.lastModified, greaterThan(Instant.now().minus(5, ChronoUnit.MINUTES)))
        assertThat(metadata.lastModified, lessThan(Instant.now().plus(5, ChronoUnit.MINUTES)))
    }

    @Test
    fun overwrite_with_new_version() {
        val coords = Managed("foo", UUID.randomUUID().toString(), "hello.txt")
        val data = "Hello world!"

        storage.putData(coords, null, MediaType.TEXT_PLAIN, data.byteInputStream())
        val data2 = "Goodbye world!"
        storage.putData(coords, null, MediaType.TEXT_PLAIN, data2.byteInputStream())

        storage.getData(coords).use {
            it!!
            assertThat(it.metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
            assertThat(it.inputStream.readTextAndClose(), equalTo(data2))
        }
    }

    @Test
    fun writes_to_separate_coords_are_separate() {
        val namespace = UUID.randomUUID().toString()
        val coordsA = Managed("foo", namespace, "hello.txt")
        val coordsB = Managed("foo", namespace,"hello2.txt")
        val dataA = "Hello world!"
        val dataB = "Goodbye world!"
        storage.putData(coordsA, null, MediaType.TEXT_PLAIN, dataA.byteInputStream())
        storage.putData(coordsB, null, MediaType.TEXT_PLAIN, dataB.byteInputStream())

        storage.getData(coordsA).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(dataA))
        }

        storage.getData(coordsB).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(dataB))
        }
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
            = this.bufferedReader(charset).use { it.readText() }

    companion object {
        @Parameterized.Parameters(name = "type: {1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(gcs , "gcs"),
            arrayOf(s3, "s3"),
            arrayOf(local, "local"))

        private val codec = SecretsCodec(DEV_MASTER_KEY_BASE64)

        private val s3 = { _: File ->
            S3StorageFactory(codec)
                .create(S3StorageFactory.Config(
                    "eu-west-1",
                    codec.encrypt(UnsafeSecret("test-howl")),
                    codec.encrypt(UnsafeSecret("arn:aws:iam::555071496850:role/Test-Bucket-Accessor")),
                    codec.encrypt(UnsafeSecret("696969"))
                ))
        }

        private val gcs = { _: File ->
            GcsStorageFactory().create(
                GcsStorageFactory.Config("howl-test.quartic.io",
                    GcsStorageFactory.Credentials.ServiceAccountJsonKey(
                        StorageFactoryShould::class.java.classLoader.getResource("howl-test-gcs.json").readText()
                    )
                )
            )
        }

        private val local = { root: File -> LocalStorage(LocalStorage.Config(root.absolutePath)) }
    }
}
