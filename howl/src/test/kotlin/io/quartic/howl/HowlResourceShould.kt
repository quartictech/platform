package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.UidGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.InputStreamWithContentType
import io.quartic.howl.storage.NoobCoords.StorageCoords
import io.quartic.howl.storage.Storage
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
    fun store_file_on_put_with_2d_coords() {
        assertPutBehavesCorrectly("/foo/thing", StorageCoords("foo", "foo", "thing"))
    }

    @Test
    fun store_file_on_put_with_3d_coords() {
        assertPutBehavesCorrectly("/foo/bar/thing", StorageCoords("foo", "bar", "thing"))
    }

    private fun assertPutBehavesCorrectly(path: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putData(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            Storage.PutResult(55)
        }

        requestManaged(path).put(Entity.text(data))

        verify(storage).putData(eq(expectedCoords), eq(data.size), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun store_file_on_post_with_2d_coords() {
        whenever(idGen.get()).thenReturn(HowlStorageId("42"))
        assertPostBehavesCorrectly("/foo", StorageCoords("foo", "foo", "42"), HowlStorageId("42"))
    }

    @Test
    fun store_file_on_post_with_3d_coords() {
        whenever(idGen.get()).thenReturn(HowlStorageId("42"))
        assertPostBehavesCorrectly("/foo/bar", StorageCoords("foo", "bar", "42"), HowlStorageId("42"))
    }

    private fun assertPostBehavesCorrectly(path: String, expectedCoords: StorageCoords, expectedId: HowlStorageId) {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(storage.putData(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            Storage.PutResult(55)
        }

        val howlStorageId = requestManaged(path).post(Entity.text(data), HowlStorageId::class.java)

        assertThat(howlStorageId, equalTo(expectedId))
        verify(storage).putData(eq(expectedCoords), eq(data.size), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun throw_if_storage_returns_null() {
        whenever(storage.putData(any(), any(), anyOrNull(), any())).thenReturn(null)
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        assertThrows<NotFoundException> {
            requestManaged("/foo/thing").post(Entity.text("noobs".toByteArray()), HowlStorageId::class.java)
        }
    }

    // See https://github.com/quartictech/platform/pull/239
    @Test
    fun cope_with_missing_content_type() {
        whenever(storage.putData(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Storage.PutResult(55))
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        requestManaged("/test").post(null, HowlStorageId::class.java)  // No entity -> missing Content-Type header

        verify(storage).putData(eq(StorageCoords("test", "test", "69")), eq(-1), eq(null), any())
    }

    @Test
    fun return_file_on_get_with_2d_coords() {
        assertGetBehavesCorrectly("/foo/thing", StorageCoords("foo", "foo", "thing"))
    }

    @Test
    fun return_file_on_get_with_3d_coords() {
        assertGetBehavesCorrectly("/foo/bar/thing", StorageCoords("foo", "bar", "thing"))
    }

    private fun assertGetBehavesCorrectly(path: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()
        whenever(storage.getData(any(), anyOrNull())).thenReturn(
            InputStreamWithContentType(MediaType.TEXT_PLAIN, ByteArrayInputStream(data))
        )

        val response = requestManaged(path).get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(storage).getData(eq(expectedCoords), eq(null))
        assertThat(responseEntity, equalTo(data))
    }

    private fun requestManaged(path: String = "") = resources.jerseyTest.target("/managed${path}").request()
}
