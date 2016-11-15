package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.uid.SequenceUidGenerator;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

public class CatalogueResourceShould {
    private final CatalogueResource resource = new CatalogueResource(SequenceUidGenerator.of(DatasetId::of), OBJECT_MAPPER);

    @Test
    public void send_current_catalogue_state_on_websocket_open() throws Exception {
        final DatasetConfig config = config("X");
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final DatasetId id = resource.registerDataset(config);

        resource.onOpen(session, mock(EndpointConfig.class));

        verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(id, config)));
    }

    @Test
    public void send_catalogue_state_to_websocket_clients_on_change() throws Exception {
        final DatasetConfig configX = config("X");
        final DatasetConfig configY = config("Y");
        final Session sessionA = mock(Session.class, RETURNS_DEEP_STUBS);
        final Session sessionB = mock(Session.class, RETURNS_DEEP_STUBS);
        resource.onOpen(sessionA, mock(EndpointConfig.class));
        resource.onOpen(sessionB, mock(EndpointConfig.class));

        final DatasetId idX = resource.registerDataset(configX);
        final DatasetId idY = resource.registerDataset(configY);
        resource.deleteDataset(idX.uid());

        newArrayList(sessionA, sessionB).forEach(session -> {
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configX)));
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configX, idY, configY)));
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configX)));
        });
    }

    @Test
    public void not_send_state_to_closed_websockets_on_change() throws Exception {
        final DatasetConfig config = config("X");
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        resource.onOpen(session, mock(EndpointConfig.class));
        resource.onClose(session, mock(CloseReason.class));

        resource.registerDataset(config);

        verify(session.getAsyncRemote()).sendText(serialize(emptyMap()));   // Initial catalogue state
        verifyNoMoreInteractions(session.getAsyncRemote());
    }

    private String serialize(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private DatasetConfig config(String name) {
        return DatasetConfig.of(
                DatasetMetadata.of(name, "bar", "baz", Optional.empty()),
                mock(DatasetLocator.class),
                emptyMap()
        );
    }
}
