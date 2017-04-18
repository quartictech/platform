package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetMetadata;
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
import java.util.Set;

public class CatalogueResource extends Endpoint implements CatalogueService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueService.class);

    private final StorageBackend storageBackend;
    private final UidGenerator<DatasetId> didGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Set<Session> sessions = Sets.newHashSet();

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
    public DatasetId registerDataset(DatasetConfig config) {
        return registerOrUpdateDataset(didGenerator.get(), config);
    }

    @Override
    public synchronized DatasetId registerOrUpdateDataset(DatasetId id, DatasetConfig config) {
        if (config.getMetadata().getRegistered() != null) {
            throw new BadRequestException("'registered' field should not be present");
        }

        // TODO: basic validation
        wrapException(() -> storageBackend.put(id, withRegisteredTimestamp(config)));
        updateClients();
        return id;
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
    public synchronized void deleteDataset(DatasetId id) {
        throwIfDatasetNotFound(id);
        wrapException(() -> storageBackend.remove(id));
        updateClients();
    }

    @Override
    public synchronized Map<DatasetId, DatasetConfig> getDatasets() {
        return wrapException(() -> ImmutableMap.copyOf(storageBackend.getAll()));
    }

    public synchronized DatasetConfig getDataset(DatasetId id) {
        throwIfDatasetNotFound(id);
        return wrapException(() -> storageBackend.get(id));
    }

    @Override
    public synchronized void onOpen(Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        updateClient(session, wrapException(storageBackend::getAll));
        sessions.add(session);
    }

    @Override
    public synchronized void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        sessions.remove(session);
    }

    private void updateClients() {
        sessions.forEach(session -> updateClient(session, wrapException(storageBackend::getAll)));
    }

    private void updateClient(Session session, Map<DatasetId, DatasetConfig> datasets) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(datasets));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }

    private void throwIfDatasetNotFound(DatasetId id) {
        try {
            if (!storageBackend.containsKey(id)) {
                throw new NotFoundException("No dataset " + id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
