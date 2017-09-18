package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.UidGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.InputStreamWithContentType
import io.quartic.howl.storage.Storage
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.apache.commons.io.IOUtils
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class HowlResourceShould {
    private val storage = mock<Storage>()
    private val idGen = mock<UidGenerator<HowlStorageId>>()

    @Rule
    @JvmField
    val resources = ResourceTestRule.builder()
        .addResource(HowlResource(storage, idGen))
        // Needed for injecting HttpServletRequest with @Context to work in the resource
        .setTestContainerFactory(GrizzlyWebTestContainerFactory())
        .build()

    @Test
    fun store_file_on_put() {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putData(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            Storage.PutResult(55)
        }

        request("foo/managed/bar/thing").put(Entity.text(data))

        verify(storage).putData(eq(Managed("foo", "bar", "thing")), eq(data.size), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun store_file_on_post() {
        whenever(idGen.get()).thenReturn(HowlStorageId("42"))
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putData(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            Storage.PutResult(55)
        }

        val howlStorageId = request("foo/managed/bar").post(Entity.text(data), HowlStorageId::class.java)

        assertThat(howlStorageId, equalTo(HowlStorageId("42")))
        verify(storage).putData(eq(Managed("foo", "bar", "42")), eq(data.size), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun throw_if_storage_returns_null() {
        whenever(storage.putData(any(), any(), anyOrNull(), any())).thenReturn(null)
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        assertThrows<NotFoundException> {
            request("foo/managed/thing").post(Entity.text("noobs".toByteArray()), HowlStorageId::class.java)
        }
    }

    // See https://github.com/quartictech/platform/pull/239
    @Test
    fun cope_with_missing_content_type() {
        whenever(storage.putData(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Storage.PutResult(55))
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        request("test/managed/thing").post(null, HowlStorageId::class.java)  // No entity -> missing Content-Type header

        verify(storage).putData(eq(Managed("test", "thing", "69")), eq(-1), eq(null), any())
    }

    @Test
    fun return_unmanaged_file_on_get() {
        assertGetBehavesCorrectly("foo/unmanaged/thing", Unmanaged("foo", "thing"))
    }

    @Test
    fun return_managed_file_on_get() {
        assertGetBehavesCorrectly("foo/managed/bar/thing", Managed("foo", "bar", "thing"))
    }

    private fun assertGetBehavesCorrectly(path: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()
        whenever(storage.getData(any(), anyOrNull())).thenReturn(
            InputStreamWithContentType(MediaType.TEXT_PLAIN, ByteArrayInputStream(data))
        )

        val response = request(path).get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(storage).getData(eq(expectedCoords), eq(null))
        assertThat(responseEntity, equalTo(data))
    }

    private fun request(path: String) = resources.jerseyTest.target(path).request()
}
