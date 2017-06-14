package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.InputStreamWithContentType
import io.quartic.howl.storage.StorageBackend
import io.quartic.howl.storage.StorageCoords
import org.apache.commons.io.IOUtils
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
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

    @Rule
    @JvmField
    val resources = ResourceTestRule.builder()
            .addResource(HowlResource(backend))
            // Needed for injecting HttpServletRequest with @Context to work in the resource
            .setTestContainerFactory(GrizzlyWebTestContainerFactory())
            .build()

    @Test
    fun store_file_on_post() {
        val data = "wat".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(backend.putData(any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(2)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            55L
        }

        val howlStorageId = resources.jerseyTest.target("/test")
                .request()
                .post(Entity.text(data), HowlStorageId::class.java)

        assertThat(howlStorageId, notNullValue())
        verify(backend).putData(eq(StorageCoords("test", "test", howlStorageId.uid)), eq(MediaType.TEXT_PLAIN), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    // See https://github.com/quartictech/platform/pull/239
    @Test
    fun cope_with_missing_content_type() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        whenever(backend.putData(any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            55L
        }

        val howlStorageId = resources.jerseyTest.target("/test")
                .request()
                .post(null, HowlStorageId::class.java)  // No entity -> missing Content-Type header

        assertThat(howlStorageId, notNullValue())
        verify(backend).putData(eq(StorageCoords("test", "test", howlStorageId.uid)), eq(null), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo("".toByteArray()))
    }

    @Test
    fun return_file_on_get() {
        val data = "wat".toByteArray()
        whenever(backend.getData(any(), anyOrNull())).thenReturn(
                InputStreamWithContentType(MediaType.TEXT_PLAIN, ByteArrayInputStream(data))
        )

        val response = resources.jerseyTest.target("/test/thing")
                .request()
                .get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(backend).getData(eq(StorageCoords("test", "test", "thing")), eq(null))
        assertThat(responseEntity, equalTo(data))
    }
}
