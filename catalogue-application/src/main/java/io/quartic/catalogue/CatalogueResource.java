package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.catalogue.api.model.DatasetNamespace;
import io.quartic.common.uid.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class CatalogueResource extends Endpoint implements CatalogueService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueService.class);

    private final StorageBackend storageBackend;
    private final UidGenerator<DatasetId> didGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Set<Session> sessions = Sets.newHashSet();    // TODO: extend ResourceManagingEndpoint instead

    public CatalogueResource(StorageBackend storageBackend, UidGenerator<DatasetId> didGenerator, Clock clock, ObjectMapper objectMapper) {
        this.storageBackend = storageBackend;
        this.didGenerator = didGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    private interface ThrowingVoid {
        void apply() throws IOException;
    }

    private <T> T wrapException(ThrowingSupplier<T> f) {
        try {
            return f.get();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void wrapException(ThrowingVoid f) {
       try {
           f.apply();
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }

    @Override
    public DatasetCoordinates registerDataset(DatasetNamespace namespace, DatasetConfig config) {
        return registerOrUpdateDataset(namespace, didGenerator.get(), config);
    }

    @Override
    public synchronized DatasetCoordinates registerOrUpdateDataset(DatasetNamespace namespace, DatasetId id, DatasetConfig config) {
        final DatasetCoordinates coords = new DatasetCoordinates(namespace, id);
        if (config.getMetadata().getRegistered() != null) {
            throw new BadRequestException("'registered' field should not be present");
        }

        // TODO: basic validation
        wrapException(() -> storageBackend.put(coords, withRegisteredTimestamp(config)));
        updateClients();
        return coords;
    }

    private DatasetConfig withRegisteredTimestamp(DatasetConfig config) {
        return new DatasetConfig(
                new DatasetMetadata(
                        config.getMetadata().getName(),
                        config.getMetadata().getDescription(),
                        config.getMetadata().getAttribution(),
                        clock.instant()
                ),
                config.getLocator(),
                config.getExtensions()
        ); // TODO - use .copy() once in Kotlin
    }

    @Override
    public synchronized Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> getDatasets() {
        return wrapException(() -> storageBackend.getAll()
                .entrySet()
                .stream()
                .collect(groupingBy(
                        e -> e.getKey().getNamespace(),
                        toMap(e -> e.getKey().getId(), Entry::getValue)
                ))
        );
    }

    public synchronized DatasetConfig getDataset(DatasetNamespace namespace, DatasetId id) {
        final DatasetCoordinates coords = new DatasetCoordinates(namespace, id);
        throwIfDatasetNotFound(coords);
        return wrapException(() -> storageBackend.get(coords));
    }

    @Override
    public synchronized void deleteDataset(DatasetNamespace namespace, DatasetId id) {
        final DatasetCoordinates coords = new DatasetCoordinates(namespace, id);
        throwIfDatasetNotFound(coords);
        wrapException(() -> storageBackend.remove(coords));
        updateClients();
    }

    @Override
    public synchronized void onOpen(Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        updateClient(session, getDatasets());
        sessions.add(session);
    }

    @Override
    public synchronized void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        sessions.remove(session);
    }

    private void updateClients() {
        final Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> datasets = getDatasets();
        sessions.forEach(session -> updateClient(session, datasets));
    }

    private void updateClient(Session session, Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> datasets) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(datasets));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }

    private void throwIfDatasetNotFound(DatasetCoordinates coords) {
        try {
            if (!storageBackend.contains(coords)) {
                throw new NotFoundException("No dataset: " + coords);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
