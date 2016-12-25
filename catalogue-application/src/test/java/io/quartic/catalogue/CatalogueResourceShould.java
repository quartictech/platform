package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetIdImpl;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadataImpl;
import io.quartic.common.uid.SequenceUidGenerator;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CatalogueResourceShould {
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private final CatalogueResource resource = new CatalogueResource(SequenceUidGenerator.of(DatasetIdImpl::of), clock, objectMapper());

    @Test(expected = BadRequestException.class)
    public void reject_registration_with_registered_timestamp_set() throws Exception {
        final DatasetConfig config = config("X", Optional.of(clock.instant()));
        resource.registerDataset(config);
    }

    @Test
    public void set_registered_timestamp_to_current_time() throws Exception {
        final DatasetConfig config = config("X");
        final DatasetId id = resource.registerDataset(config);

        assertThat(resource.getDataset(id).metadata().registered(), equalTo(Optional.of(clock.instant())));
    }

    @Test
    public void send_current_catalogue_state_on_websocket_open() throws Exception {
        final DatasetConfig config = config("X");
        final DatasetConfig configWithTimestamp = config("X", Optional.of(clock.instant()));

        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final DatasetId id = resource.registerDataset(config);

        resource.onOpen(session, mock(EndpointConfig.class));

        verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(id, configWithTimestamp)));
    }

    @Test
    public void send_catalogue_state_to_websocket_clients_on_change() throws Exception {
        final DatasetConfig configX = config("X");
        final DatasetConfig configY = config("Y");
        final DatasetConfig configWithTimestampX = config("X", Optional.of(clock.instant()));
        final DatasetConfig configWithTimestampY = config("Y", Optional.of(clock.instant()));

        final Session sessionA = mock(Session.class, RETURNS_DEEP_STUBS);
        final Session sessionB = mock(Session.class, RETURNS_DEEP_STUBS);
        resource.onOpen(sessionA, mock(EndpointConfig.class));
        resource.onOpen(sessionB, mock(EndpointConfig.class));

        final DatasetId idX = resource.registerDataset(configX);
        final DatasetId idY = resource.registerDataset(configY);
        resource.deleteDataset(idX);

        newArrayList(sessionA, sessionB).forEach(session -> {
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configWithTimestampX)));
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configWithTimestampX, idY, configWithTimestampY)));
            verify(session.getAsyncRemote()).sendText(serialize(ImmutableMap.of(idX, configWithTimestampX)));
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
            return objectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private DatasetConfig config(String name) {
        return config(name, Optional.empty());
    }

    private DatasetConfig config(String name, Optional<Instant> instant) {
        return DatasetConfigImpl.of(
                DatasetMetadataImpl.of(name, "bar", "baz", instant, Optional.empty()),
                mock(DatasetLocator.class),
                emptyMap()
        );
    }
}
