package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.UidGenerator
import io.quartic.howl.api.HowlClient.Companion.UNMANAGED_SOURCE_KEY_HEADER
import io.quartic.howl.api.model.HowlStorageId
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage
import io.quartic.howl.storage.Storage.StorageResult
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import io.quartic.howl.storage.StorageFactory
import org.apache.commons.io.IOUtils
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.ws.rs.ClientErrorException
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.Entity
import javax.ws.rs.core.HttpHeaders.*
import javax.ws.rs.core.MediaType

class HowlResourceShould {
    private val storage = mock<Storage>()
    private val storageFactory = mock<StorageFactory> {
        on { createFor("foo") } doReturn storage
    }
    private val idGen = mock<UidGenerator<HowlStorageId>>()

    @Rule
    @JvmField
    val resources = ResourceTestRule.builder()
        .addResource(HowlResource(storageFactory, idGen))
        // Needed for injecting HttpServletRequest with @Context to work in the resource
        .setTestContainerFactory(GrizzlyWebTestContainerFactory())
        .setMapper(OBJECT_MAPPER)
        .build()

    @Test
    fun store_object_on_put() {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putObject(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(2)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            storageMetadata()
        }

        request("foo/managed/bar/thing").put(Entity.text(data), Unit::class.java)

        verify(storage).putObject(eq(data.size), eq(MediaType.TEXT_PLAIN), any(), eq(Managed("bar", "thing")))
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun store_object_on_post() {
        whenever(idGen.get()).thenReturn(HowlStorageId("42"))
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putObject(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(2)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            storageMetadata()
        }

        val howlStorageId = request("foo/managed/bar").post(Entity.text(data), HowlStorageId::class.java)

        assertThat(howlStorageId, equalTo(HowlStorageId("42")))
        verify(storage).putObject(eq(data.size), eq(MediaType.TEXT_PLAIN), any(), eq(Managed("bar", "42")))
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun copy_object_on_put_with_source_key_header() {
        whenever(storage.copyObject(any(), any(), anyOrNull())).thenReturn(storageMetadata())

        val metadata = request("foo/managed/bar/thing")
            .header(UNMANAGED_SOURCE_KEY_HEADER, "weird")
            .put(Entity.text(""), StorageMetadata::class.java)

        assertThat(metadata, equalTo(storageMetadata()))
        verify(storage).copyObject(Unmanaged("weird"), Managed("bar", "thing"))
    }

    @Test
    fun copy_object_on_put_with_source_key_header_if_different_etag_specified() {
        whenever(storage.copyObject(any(), any(), anyOrNull())).thenReturn(storageMetadata())

        val metadata = request("foo/managed/bar/thing")
            .header(UNMANAGED_SOURCE_KEY_HEADER, "weird")
            .header(IF_NONE_MATCH, "leet")
            .put(Entity.text(""), StorageMetadata::class.java)

        assertThat(metadata, equalTo(storageMetadata()))
        verify(storage).copyObject(Unmanaged("weird"), Managed("bar", "thing"), "leet")
    }

    @Test
    fun throw_precondition_failed_if_matching_etag_specified() {
        whenever(storage.copyObject(any(), any(), anyOrNull())).thenReturn(storageMetadata())

        val ex = assertThrows<ClientErrorException> {
            request("foo/managed/bar/thing")
                .header(UNMANAGED_SOURCE_KEY_HEADER, "weird")
                .header(IF_NONE_MATCH, "noob")
                .put(Entity.text(""), String::class.java)
        }
        assertThat(ex.response.status, equalTo(412))
    }

    @Test
    fun throw_not_found_if_copy_source_not_present() {
        whenever(storage.copyObject(any(), any(), anyOrNull())).thenReturn(null)

        assertThrows<NotFoundException> {
            request("foo/managed/bar/thing")
                .header(UNMANAGED_SOURCE_KEY_HEADER, "weird")
                .put(Entity.text(""), String::class.java)
        }
    }

    @Test
    fun throw_not_found_if_storage_not_present() {
        whenever(storageFactory.createFor(any())).thenReturn(null)

        assertThrows<NotFoundException> {
            request("foo/managed/thing").post(Entity.text("noobs".toByteArray()), HowlStorageId::class.java)
        }
    }

    // See https://github.com/quartictech/platform/pull/239
    @Test
    fun cope_with_missing_content_type() {
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        request("foo/managed/thing").post(null, HowlStorageId::class.java)  // No entity -> missing Content-Type header

        verify(storage).putObject(eq(-1), eq(null), any(), eq(Managed("thing", "69")))
    }

    @Test
    fun return_unmanaged_object_on_get() {
        assertGetBehavesCorrectly("foo/unmanaged/thing", Unmanaged("thing"))
    }

    @Test
    fun return_managed_object_on_get() {
        assertGetBehavesCorrectly("foo/managed/bar/thing", Managed("bar", "thing"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun return_metadata_on_head() {
        whenever(storage.getMetadata(any())).thenReturn(storageMetadata())

        val response = request("foo/unmanaged/wat").head()
        assertThat(response.headers[CONTENT_TYPE] as List<String>, equalTo(listOf(MediaType.TEXT_PLAIN)))
        assertThat(response.headers[CONTENT_LENGTH] as List<String>, equalTo(listOf("3")))
        assertThat(response.headers[ETAG] as List<String>, equalTo(listOf("noob")))
    }

    private fun assertGetBehavesCorrectly(path: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()
        whenever(storage.getObject(any())).thenReturn(
            StorageResult(
                StorageMetadata(MediaType.TEXT_PLAIN, 3, "noob"),
                ByteArrayInputStream(data)
            )
        )

        val response = request(path).get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(storage).getObject(expectedCoords)
        assertThat(responseEntity, equalTo(data))
    }

    private fun storageMetadata() = StorageMetadata(MediaType.TEXT_PLAIN, 3, "noob")

    private fun request(path: String) = resources.jerseyTest.target(path).request()
}
