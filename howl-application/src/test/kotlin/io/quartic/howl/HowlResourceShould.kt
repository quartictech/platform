package io.quartic.howl

import com.nhaarman.mockito_kotlin.*
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.InputStreamWithContentType
import io.quartic.howl.storage.StorageBackend
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

        whenever(backend.putData(any(), any(), any(), any())).thenAnswer { invocation ->
            val inputStream = invocation.getArgument<InputStream>(3)
            IOUtils.copy(inputStream, byteArrayOutputStream)
            55L
        }

        val howlStorageId = resources.jerseyTest.target("/test")
                .request()
                .post(Entity.entity(data, MediaType.TEXT_PLAIN_TYPE), HowlStorageId::class.java)

        assertThat(howlStorageId, notNullValue())
        verify(backend).putData(eq(MediaType.TEXT_PLAIN), eq("test"), eq(howlStorageId.uid), any())
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data))
    }

    @Test
    fun return_file_on_get() {
        val data = "wat".toByteArray()
        whenever(backend.getData(any(), any(), anyOrNull())).thenReturn(
                InputStreamWithContentType(MediaType.TEXT_PLAIN, ByteArrayInputStream(data))
        )

        val response = resources.jerseyTest.target("/test/thing")
                .request()
                .get()

        val responseEntity = response.readEntity(ByteArray::class.java)
        verify(backend).getData(eq("test"), eq("thing"), eq(null))
        assertThat(responseEntity, equalTo(data))
    }
}
