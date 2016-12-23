package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetMetadataImpl;
import io.quartic.common.uid.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;

public class CatalogueResource extends Endpoint implements CatalogueService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueService.class);

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();
    private final UidGenerator<DatasetId> didGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Set<Session> sessions = Sets.newHashSet();

    public CatalogueResource(UidGenerator<DatasetId> didGenerator, Clock clock, ObjectMapper objectMapper) {
        this.didGenerator = didGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public synchronized DatasetId registerDataset(DatasetConfig config) {
        if (config.metadata().registered().isPresent()) {
            throw new BadRequestException("'registered' field should not be present");
        }

        // TODO: basic validation
        DatasetId id = didGenerator.get();
        datasets.put(id, withRegisteredTimestamp(config));
        updateClients();
        return id;
    }

    private DatasetConfig withRegisteredTimestamp(DatasetConfig config) {
        return DatasetConfigImpl.copyOf(config)
                .withMetadata(DatasetMetadataImpl.copyOf(config.metadata())
                        .withRegistered(clock.instant())
                );
    }

    public synchronized void deleteDataset(DatasetId id) {
        throwIfDatasetNotFound(id);
        datasets.remove(id);
        updateClients();
    }

    public synchronized Map<DatasetId, DatasetConfig> getDatasets() {
        return ImmutableMap.copyOf(datasets);
    }

    public synchronized DatasetConfig getDataset(DatasetId id) {
        throwIfDatasetNotFound(id);
        return datasets.get(id);
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
