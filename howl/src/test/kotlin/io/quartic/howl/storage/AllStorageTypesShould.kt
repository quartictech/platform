package io.quartic.howl.storage

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.test.IntegrationTest
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

@IntegrationTest
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
        val etag = storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        storage.getObject(coords).use {
            it!!
            assertThat(it.inputStream.readTextAndClose(), equalTo(data))
            assertMetadataCorrect(it.metadata, etag)
        }
    }

    @Test
    fun get_just_metadata_from_previous_put() {
        val etag = storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        assertMetadataCorrect(storage.getMetadata(coords)!!, etag)
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
        val etag = storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)

        val destCoords = Managed(namespace, "hello2.txt")
        val etagCopy = storage.copyObject(coords, destCoords)!!

        assertThat(etagCopy, equalTo(etag))
        assertMetadataCorrect(storage.getMetadata(destCoords)!!, etagCopy)
    }

    @Test
    fun return_null_if_copy_source_not_found() {
        val destCoords = Managed(namespace, "hello2.txt")

        assertThat(storage.copyObject(coords, destCoords), nullValue())
    }

    @Test
    fun return_etags_that_are_based_only_on_object_content() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
        val originalEtag = storage.getObject(coords)!!.metadata.etag

        // Overwrite with same data
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
        assertThat(storage.getObject(coords)!!.metadata.etag, equalTo(originalEtag))

        // Write at different location
        val otherCoords = Managed(namespace, "hello2.txt")
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), otherCoords)
        assertThat(storage.getObject(otherCoords)!!.metadata.etag, equalTo(originalEtag))

        // Different for different data
        val replacementData = "Goodbye world!"
        storage.putObject(replacementData.length, MediaType.TEXT_PLAIN, replacementData.byteInputStream(), coords)
        assertThat(storage.getObject(coords)!!.metadata.etag, not(equalTo(originalEtag)))
    }

    @Test
    fun copy_object_if_etag_specified_but_mismatches() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
        val etag = storage.getObject(coords)!!.metadata.etag

        val destCoords = Managed(namespace, "hello2.txt")

        assertThat(storage.copyObject(coords, destCoords, "different-etag"), equalTo(etag))
        assertMetadataCorrect(storage.getMetadata(destCoords)!!, etag)
    }

    @Test
    fun not_copy_object_if_etag_specified_and_matches() {
        storage.putObject(data.length, MediaType.TEXT_PLAIN, data.byteInputStream(), coords)
        val etag = storage.getObject(coords)!!.metadata.etag

        val destCoords = Managed(namespace, "hello2.txt")

        assertThat(storage.copyObject(coords, destCoords, etag), equalTo(etag))
        assertThat(storage.getMetadata(destCoords), nullValue())
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

    private fun assertMetadataCorrect(metadata: StorageMetadata, expectedEtag: String) {
        assertThat(metadata.contentLength, equalTo(data.length.toLong()))
        assertThat(metadata.contentType, equalTo(MediaType.TEXT_PLAIN))
        assertThat(metadata.lastModified, greaterThan(Instant.now() - Duration.ofMinutes(5)))
        assertThat(metadata.lastModified, lessThan(Instant.now() + Duration.ofMinutes(5)))
        assertThat(metadata.etag, equalTo(expectedEtag))
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
            arrayOf(local, "local")
        )

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
