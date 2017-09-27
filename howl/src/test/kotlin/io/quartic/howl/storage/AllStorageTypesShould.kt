package io.quartic.howl.storage

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.GcsStorage.Config.Credentials.ServiceAccountJsonKey
import io.quartic.howl.storage.StorageCoords.Managed
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType

@RunWith(Parameterized::class)
class AllStorageTypesShould {
    @Parameter
    lateinit var storageFactory: (File) -> Storage

    // Needed to keep junit happy
    @Parameter(1)
    lateinit var name: String

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    private val storage by lazy { storageFactory(folder.root) }

    @Test
    fun get_object_and_metadata_from_previous_put() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        storage.getObject(coords).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(data))
            assertMetadataCorrect(it.metadata)
        }
    }

    @Test
    fun get_just_metadata_from_previous_put() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        assertMetadataCorrect(storage.getMetadata(coords)!!)
    }

    @Test
    fun ignore_content_length_if_negative() {
        storage.putObject(-1, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
    }

    @Test
    fun return_null_if_key_not_found() {
        assertThat(storage.getObject(coords), nullValue())
    }

    @Test
    fun return_null_metadata_if_key_not_found() {
        assertThat(storage.getMetadata(coords), nullValue())
    }

    @Test
    fun copy_object_from_previous_put() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        val destCoords = Managed(namespace, "hello2.txt")

        assertMetadataCorrect(storage.copyObject(coords, destCoords)!!)
        assertMetadataCorrect(storage.getMetadata(destCoords)!!)
    }

    @Test
    fun return_null_if_copy_source_not_found() {
        val destCoords = Managed(namespace, "hello2.txt")

        assertThat(storage.copyObject(coords, destCoords), nullValue())
    }

    @Test
    fun overwrite_with_new_version() {
        storage.putObject(null, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        val replacementData = "Goodbye world!"
        storage.putObject(null, MediaType.TEXT_PLAIN, replacementData.byteInputStream(), coords)

        storage.getObject(coords).use {
            it!!
            assertThat(it.metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
            assertThat(it.inputStream.readTextAndClose(), equalTo(replacementData))
        }
    }

    @Test
    fun write_to_separate_objects_for_separate_coords() {
        storage.putObject(null, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        val otherCoords = Managed(namespace, "hello2.txt")
        val otherData = "Goodbye world!"
        storage.putObject(null, MediaType.TEXT_PLAIN, otherData.byteInputStream(), otherCoords)

        storage.getObject(coords).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(data))
        }

        storage.getObject(otherCoords).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(otherData))
        }
    }

    private fun randomNamespace() = UUID.randomUUID().toString()

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8)
        = this.bufferedReader(charset).use { it.readText() }

    private fun assertMetadataCorrect(metadata: StorageMetadata) {
        assertThat(metadata.contentLength, equalTo(data.length.toLong()))
        assertThat(metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(metadata.lastModified, greaterThan(Instant.now() - Duration.ofMinutes(5)))
        assertThat(metadata.lastModified, lessThan(Instant.now() + Duration.ofMinutes(5)))
    }

    private val data = "Hello world!"
    private val namespace = randomNamespace()
    private val coords = Managed(namespace, "hello.txt")

    companion object {
        @Parameters(name = "type: {1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(gcs , "gcs"),
            arrayOf(s3, "s3"),
            arrayOf(local, "local"))

        private val codec = SecretsCodec(DEV_MASTER_KEY_BASE64)

        private val s3 = { _: File ->
            S3Storage.Factory(codec).create(S3Storage.Config(
                "eu-west-1",
                codec.encrypt(UnsafeSecret("test-howl")),
                codec.encrypt(UnsafeSecret("arn:aws:iam::555071496850:role/Test-Bucket-Accessor")),
                codec.encrypt(UnsafeSecret("696969"))
            ))
        }

        private val gcs = { _: File ->
            GcsStorage.Factory().create(
                GcsStorage.Config("howl-test.quartic.io",
                    ServiceAccountJsonKey(
                        AllStorageTypesShould::class.java.classLoader.getResource("howl-test-gcs.json").readText()
                    )
                )
            )
        }

        private val local = { root: File -> LocalStorage(LocalStorage.Config(root.absolutePath)) }
    }
}
