package io.quartic.weyl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.live.WebsocketLiveImporter;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebsocketImporterService {
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<LayerId, WebsocketLiveImporter> importers;
    private final LayerStore layerStore;

    public WebsocketImporterService(LayerStore layerStore, UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator, ObjectMapper objectMapper) {
        this.layerStore = layerStore;
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
        this.objectMapper = objectMapper;
        this.importers = new ConcurrentHashMap<>();
    }

    public synchronized WebsocketLiveImporter start(URI uri, LayerId layerId) {
        return importers.computeIfAbsent(layerId, (id) -> WebsocketLiveImporter.start(uri, id, fidGenerator, eidGenerator, layerStore, objectMapper));
    }
}
