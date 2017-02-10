package io.quartic.howl;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.howl.storage.InputStreamWithContentTypeImpl;
import io.quartic.howl.storage.StorageBackend;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HowlResourceShould {
    private static final StorageBackend backend = mock(StorageBackend.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new HowlResource(backend))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();

    @Test
    public void store_file_on_post() throws IOException {
        byte[] data = "wat".getBytes();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        when(backend.put(any(), any(), any(), any())).thenAnswer(invocation -> {
            InputStream inputStream = invocation.getArgument(3);
            IOUtils.copy(inputStream, byteArrayOutputStream);
            return 55L;
        });

        HowlStorageId howlStorageId = resources.getJerseyTest().target("/test")
                .request()
                .post(Entity.entity(data, MediaType.TEXT_PLAIN_TYPE), HowlStorageId.class);

        assertThat(howlStorageId, notNullValue());
        verify(backend).put(eq(MediaType.TEXT_PLAIN), eq("test"), eq(howlStorageId.getUid()), any());
        assertThat(byteArrayOutputStream.toByteArray(), equalTo(data));
    }

    @Test
    public void return_file_on_get() throws IOException {
        byte[] data = "wat".getBytes();
        when(backend.get(any(), any(), any())).thenAnswer(invocation -> {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            return Optional.of(InputStreamWithContentTypeImpl.of(MediaType.TEXT_PLAIN, byteArrayInputStream));
        });

        Response response = resources.getJerseyTest().target("/test/thing")
                .request()
                .get();
        byte[] responseEntity = response.readEntity(byte[].class);
        assertThat(responseEntity, equalTo(data));
    }
}
