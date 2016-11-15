package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.weyl.common.uid.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.NotFoundException;
import java.util.Map;
import java.util.Set;

public class CatalogueResource extends Endpoint implements CatalogueService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueService.class);

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();
    private final UidGenerator<DatasetId> didGenerator;
    private final ObjectMapper objectMapper;
    private final Set<Session> sessions = Sets.newHashSet();

    public CatalogueResource(UidGenerator<DatasetId> didGenerator, ObjectMapper objectMapper) {
        this.didGenerator = didGenerator;
        this.objectMapper = objectMapper;
    }

    public synchronized DatasetId registerDataset(DatasetConfig config) {
        // TODO: basic validation
        DatasetId id = didGenerator.get();
        datasets.put(id, config);
        updateClients();
        return id;
    }

    public synchronized void deleteDataset(String id) {
        final DatasetId did = DatasetId.of(id);
        throwIfDatasetNotFound(did);
        datasets.remove(did);
        updateClients();
    }

    public synchronized Map<DatasetId, DatasetConfig> getDatasets() {
        return ImmutableMap.copyOf(datasets);
    }

    public synchronized DatasetConfig getDataset(String id) {
        final DatasetId did = DatasetId.of(id);
        throwIfDatasetNotFound(did);
        return datasets.get(did);
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
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(datasets));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }

    private void throwIfDatasetNotFound(DatasetId id) {
        if (!datasets.containsKey(id)) {
            throw new NotFoundException("No dataset " + id);
        }
    }
}
