package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.catalogue.api.model.DatasetNamespace;
import io.quartic.catalogue.api.model.GeoJsonDatasetLocator;
import org.junit.Test;
import org.mockito.InOrder;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Map.Entry;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.common.uid.UidUtilsKt.sequenceGenerator;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CatalogueResourceShould {
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private final StorageBackend backend = mock(StorageBackend.class);
    private final CatalogueResource resource = new CatalogueResource(
            backend,
            sequenceGenerator(DatasetId::new),
            clock,
            objectMapper()
    );
    private final DatasetNamespace namespace = new DatasetNamespace("foo");

    @Test
    public void get_specific_dataset() throws Exception {
        final DatasetId id = mock(DatasetId.class);
        final DatasetConfig config = mock(DatasetConfig.class);

        when(backend.contains(coords(namespace, id))).thenReturn(true);
        when(backend.get(coords(namespace, id))).thenReturn(config);

        assertThat(resource.getDataset(namespace, id), equalTo(config));
    }

    @Test
    public void get_all_datasets() throws Exception {
        final Map<DatasetCoordinates, DatasetConfig> datasets = loadsOfDatasets();

        when(backend.getAll()).thenReturn(datasets);

        assertThat(resource.getDatasets(), equalTo(
                datasets.entrySet()
                .stream()
                .collect(groupingBy(e -> e.getKey().getNamespace(), toMap(e -> e.getKey().getId(), Entry::getValue)))
        ));
    }

    @Test(expected = BadRequestException.class)
    public void reject_registration_with_registered_timestamp_set() throws Exception {
        final DatasetConfig config = config(clock.instant());

        resource.registerDataset(namespace, config);
    }

    @Test
    public void set_registered_timestamp_to_current_time() throws Exception {
        final DatasetConfig config = config();
        final DatasetConfig configWithTimestamp = config(clock.instant());

        final DatasetCoordinates coords = resource.registerDataset(namespace, config);

        verify(backend).put(coords, configWithTimestamp);
    }

    @Test
    public void register_dataset_with_specified_id() throws Exception {
        final DatasetConfig config = config();
        final DatasetConfig configWithTimestamp = config(clock.instant());

        final DatasetId id = new DatasetId("123");
        resource.registerOrUpdateDataset(namespace, id, config);

        verify(backend).put(coords(namespace, id), configWithTimestamp);
    }

    @Test
    public void send_current_catalogue_state_on_websocket_open() throws Exception {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final Map<DatasetCoordinates, DatasetConfig> datasets = loadsOfDatasets();

        when(backend.getAll()).thenReturn(datasets);

        resource.onOpen(session, mock(EndpointConfig.class));

        verify(session.getAsyncRemote()).sendText(serialize(toMapOfMaps(datasets)));
    }

    @Test
    public void send_catalogue_state_to_websocket_clients_after_change() throws Exception {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        final Map<DatasetCoordinates, DatasetConfig> datasets = loadsOfDatasets();

        when(backend.getAll())
                .thenReturn(emptyMap())
                .thenReturn(datasets);

        resource.onOpen(session, mock(EndpointConfig.class));
        resource.registerDataset(namespace, config());

        verify(session.getAsyncRemote()).sendText(serialize(toMapOfMaps(datasets)));

        final InOrder inOrder = inOrder(backend);
        inOrder.verify(backend).getAll();   // This happens on session open
        inOrder.verify(backend).put(any(), any());
        inOrder.verify(backend).getAll();   // We care that this happened *after* the put
    }

    @Test
    public void not_send_state_to_closed_websockets_after_change() throws Exception {
        final Session session = mock(Session.class, RETURNS_DEEP_STUBS);

        resource.onOpen(session, mock(EndpointConfig.class));
        resource.onClose(session, mock(CloseReason.class));
        resource.registerDataset(namespace, config());

        verify(session.getAsyncRemote()).sendText(serialize(emptyMap()));   // Initial catalogue state
        verifyNoMoreInteractions(session.getAsyncRemote());
    }

    private Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> toMapOfMaps(Map<DatasetCoordinates, DatasetConfig> map) {
        return map.entrySet()
                .stream()
                .collect(groupingBy(
                        e -> e.getKey().getNamespace(),
                        toMap(e -> e.getKey().getId(), Entry::getValue)
                ));
    }

    private String serialize(Object object) {
        try {
            return objectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private DatasetConfig config() {
        return config(null);
    }

    private DatasetConfig config(Instant instant) {
        return new DatasetConfig(
                new DatasetMetadata("foo", "bar", "baz", instant),
                new GeoJsonDatasetLocator("blah"),
                emptyMap()
        );
    }

    private DatasetCoordinates coords(DatasetNamespace namespace, DatasetId id) {
        return new DatasetCoordinates(namespace, id);
    }

    private Map<DatasetCoordinates, DatasetConfig> loadsOfDatasets() {
        final DatasetNamespace otherNamespace = new DatasetNamespace("bar");   // Prove that multiple namespaces work ok
        return map(
                entry(coords(namespace, mock(DatasetId.class)), mock(DatasetConfig.class)),
                entry(coords(namespace, mock(DatasetId.class)), mock(DatasetConfig.class)),
                entry(coords(otherNamespace, mock(DatasetId.class)), mock(DatasetConfig.class))
        );
    }
}
