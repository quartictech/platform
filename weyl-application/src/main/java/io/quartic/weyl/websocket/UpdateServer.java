package io.quartic.weyl.websocket;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.websocket.message.*;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static io.quartic.common.uid.UidUtils.stringify;
import static io.quartic.weyl.core.feature.FeatureConverter.getRawProperties;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener, GeofenceListener {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;
    private final Set<Violation> violations = newLinkedHashSet();
    private final List<LayerSubscription> subscriptions = newArrayList();
    private final List<ClientStatusMessageHandler> handlers;
    private final GeofenceStore geofenceStore;
    private final AlertProcessor alertProcessor;
    private final LayerStore layerStore;
    private Session session;

    public UpdateServer(
            LayerStore layerStore,
            GeofenceStore geofenceStore,
            AlertProcessor alertProcessor,
            Collection<ClientStatusMessageHandler.Factory> handlerFactories,
            GeometryTransformer geometryTransformer,
            ObjectMapper objectMapper
    ) {
        this.layerStore = layerStore;
        this.geofenceStore = geofenceStore;
        this.alertProcessor = alertProcessor;
        this.handlers = handlerFactories.stream().map(f -> f.create(this::sendMessage)).collect(toList());
        this.geometryTransformer = geometryTransformer;
        this.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        this.session = session;
        alertProcessor.addListener(this);
        geofenceStore.addListener(this);
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            final SocketMessage msg = objectMapper.readValue(message, SocketMessage.class);
            if (msg instanceof ClientStatusMessage) {
                ClientStatusMessage csm = (ClientStatusMessage)msg;
                LOG.info("[{}] Subscribed to layers {} + entities {}",
                        session.getId(), stringify(csm.subscribedLiveLayerIds()), stringify(csm.selection().entityIds()));
                unsubscribeAll();
                csm.subscribedLiveLayerIds().forEach(this::subscribe);
                handlers.forEach(t -> t.onClientStatusMessage(csm));
            } else if (msg instanceof PingMessage) {
                LOG.info("[{}] Received ping", session.getId());
            } else {
                throw new RuntimeException("Unrecognised type '" + msg.getClass().getCanonicalName() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        alertProcessor.removeListener(this);
        geofenceStore.removeListener(this);
        handlers.forEach(t -> {
            try {
                t.close();
            } catch (Exception e) {
                LOG.error("Could not close handler", e);
            }
        });
        unsubscribeAll();
    }

    @Override
    public void onAlert(Alert alert) {
        sendMessage(AlertMessageImpl.of(alert));
    }

    @Override
    public void onViolationBegin(Violation violation) {
        synchronized (violations) {
            violations.add(violation);
            sendViolationsUpdate();
        }
    }

    @Override
    public void onViolationEnd(Violation violation) {
        synchronized (violations) {
            violations.remove(violation);
            sendViolationsUpdate();
        }
    }

    @Override
    public void onGeometryChange(Collection<Feature> features) {
        sendMessage(GeofenceGeometryUpdateMessageImpl.of(fromJts(features)));
    }

    private void unsubscribeAll() {
        subscriptions.forEach(layerStore::removeSubscriber);
        subscriptions.clear();
    }

    private void subscribe(LayerId layerId) {
        subscriptions.add(layerStore.addSubscriber(layerId, state -> sendLayerUpdate(layerId, state)));
    }

    private void sendViolationsUpdate() {
        sendMessage(GeofenceViolationsUpdateMessageImpl.of(violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList())));
    }

    private void sendLayerUpdate(LayerId layerId, LayerState state) {
        sendMessage(LayerUpdateMessageImpl.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(fromJts(state.featureCollection()))  // TODO: obviously we never want to do this with large static layers
                .build()
        );
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }

    private FeatureCollection fromJts(Collection<Feature> features) {
        return FeatureCollectionImpl.of(
                features.stream()
                        .map(this::fromJts)
                        .collect(toList())
        );
    }

    private io.quartic.geojson.Feature fromJts(Feature f) {
        return FeatureImpl.of(
                Optional.empty(),
                Optional.of(Utils.fromJts(geometryTransformer.transform(f.geometry()))),
                getRawProperties(f)
        );
    }
}
