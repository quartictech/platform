package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.uid.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CatalogueResource extends Endpoint implements CatalogueService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueService.class);

    private final StorageBackend storageBackend;
    private final UidGenerator<DatasetId> didGenerator;
    private final ObjectMapper objectMapper;
    private final Set<Session> sessions = Sets.newHashSet();

    public CatalogueResource(StorageBackend storageBackend, UidGenerator<DatasetId> didGenerator,
                             ObjectMapper objectMapper) {
        this.storageBackend = storageBackend;
        this.didGenerator = didGenerator;
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
    public synchronized DatasetId registerDataset(DatasetConfig config) {
        // TODO: basic validation
        DatasetId id = didGenerator.get();
        wrapException(() -> storageBackend.put(id, config));
        updateClients();
        return id;
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
        updateClient(session);
        sessions.add(session);
    }

    @Override
    public synchronized void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        sessions.remove(session);
    }

    private void updateClients() {
        sessions.forEach(this::updateClient);
    }

    private void updateClient(Session session) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(
                    wrapException(storageBackend::getAll)));
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
