package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.common.uid.UidGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.InputStreamWithContentType
import io.quartic.howl.storage.StorageBackend
import io.quartic.howl.storage.StorageCoords
import org.apache.commons.io.IOUtils
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class HowlResourceShould {
    private val backend = mock<StorageBackend>()
    private val idGen = mock<UidGenerator<HowlStorageId>>()

    @Rule
    @JvmField
    val resources = ResourceTestRule.builder()
            .addResource(HowlResource(backend, idGen))
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

    private fun assertPutBehavesCorrectly(requestPath: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(backend.putData(any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(2)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            55L
        }

        resources.jerseyTest.target(requestPath)
                .request()
                .put(Entity.text(data))

        verify(backend).putData(eq(expectedCoords), eq(MediaType.TEXT_PLAIN), any())
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

    private fun assertPostBehavesCorrectly(requestPath: String, expectedCoords: StorageCoords, expectedId: HowlStorageId) {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(backend.putData(any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(2)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            55L
        }

        val howlStorageId = resources.jerseyTest.target(requestPath)
                .request()
                .post(Entity.text(data), HowlStorageId::class.java)

        assertThat(howlStorageId, equalTo(expectedId))
        verify(backend).putData(eq(expectedCoords), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    // See https://github.com/quartictech/platform/pull/239
    @Test
    fun cope_with_missing_content_type() {
        whenever(idGen.get()).thenReturn(HowlStorageId("69"))

        resources.jerseyTest.target("/test")
                .request()
                .post(null, HowlStorageId::class.java)  // No entity -> missing Content-Type header

        verify(backend).putData(eq(StorageCoords("test", "test", "69")), eq(null), any())
    }

    @Test
    fun return_file_on_get_with_2d_coords() {
        assertGetBehavesCorrectly("/foo/thing", StorageCoords("foo", "foo", "thing"))
    }

    @Test
    fun return_file_on_get_with_3d_coords() {
        assertGetBehavesCorrectly("/foo/bar/thing", StorageCoords("foo", "bar", "thing"))
    }

    private fun assertGetBehavesCorrectly(requestPath: String, expectedCoords: StorageCoords) {
        val data = "wat".toByteArray()
        whenever(backend.getData(any(), anyOrNull())).thenReturn(
                InputStreamWithContentType(MediaType.TEXT_PLAIN, ByteArrayInputStream(data))
        )

        val response = resources.jerseyTest.target(requestPath)
                .request()
                .get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(backend).getData(eq(expectedCoords), eq(null))
        assertThat(responseEntity, equalTo(data))
    }
}
