package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.*;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener, GeofenceListener {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateServer.class);
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;
    private final Set<Violation> violations = newHashSet();
    private final List<LayerSubscription> subscriptions = newArrayList();
    private final GeofenceStore geofenceStore;
    private final AlertProcessor alertProcessor;
    private LayerStore layerStore;
    private Session session;

    public UpdateServer(LayerStore layerStore, GeofenceStore geofenceStore, AlertProcessor alertProcessor, GeometryTransformer geometryTransformer, ObjectMapper objectMapper) {
        this.layerStore = layerStore;
        this.geofenceStore = geofenceStore;
        this.alertProcessor = alertProcessor;
        this.geometryTransformer = geometryTransformer;
        this.objectMapper = objectMapper;
        LOG.info("Creating UpdateServer");
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
                LOG.info("[{}] Subscribed to {}", session.getId(), csm.subscribedLiveLayerIds());
                unsubscribeAll();
                csm.subscribedLiveLayerIds().forEach(this::subscribe);
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
        unsubscribeAll();
    }

    @Override
    public void onAlert(AbstractAlert alert) {
        sendMessage(AlertMessage.of(alert));
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
    public void onGeometryChange(Collection<io.quartic.weyl.core.model.Feature> features) {
        sendMessage(GeofenceGeometryUpdateMessage.of(fromJts(features, f -> f.externalId())));  // TODO: it's silly that we use externalId for geofences, and fid for everything else
    }

    private void unsubscribeAll() {
        subscriptions.forEach(layerStore::removeSubscriber);
        subscriptions.clear();
    }

    private void subscribe(LayerId layerId) {
        subscriptions.add(layerStore.addSubscriber(layerId, state -> sendLayerUpdate(layerId, state)));
    }

    private void sendViolationsUpdate() {
        sendMessage(GeofenceViolationsUpdateMessage.of(violations.stream().map(Violation::geofenceId).collect(toList())));
    }

    private void sendLayerUpdate(LayerId layerId, LayerState state) {
        sendMessage(LayerUpdateMessage.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(fromJts(state.featureCollection(), f -> f.uid().uid()))  // TODO: obviously we never want to do this with large static layers
                .feedEvents(state.feedEvents())
                .externalIdToFeatureIdMapping(computeExternalIdToFeatureIdMapping(state))
                .build()
        );
    }

    // TODO: This is a hack for live update of selection. We are also assuming uid monotonic increasing which is NAUGHTY
    private Map<String, ? extends FeatureId> computeExternalIdToFeatureIdMapping(LayerState state) {
        return state.featureCollection().stream()
                .collect(groupingBy(io.quartic.weyl.core.model.Feature::externalId))
                .entrySet()
                .stream()
                // HACK!!!
                .collect(toMap(Map.Entry::getKey, entry -> Iterables.getFirst(entry.getValue(), null).uid()));
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();    // TODO
        }
    }

    private FeatureCollection fromJts(Collection<io.quartic.weyl.core.model.Feature> features, Function<io.quartic.weyl.core.model.Feature, String> id) {
        return FeatureCollection.of(
                features.stream()
                        .map(f -> fromJts(f, id.apply(f)))
                        .collect(toList())
        );
    }

    private Feature fromJts(io.quartic.weyl.core.model.Feature f, String id) {
        return fromJts(Optional.of(f.externalId()), f.geometry(), convertMetadata(f.externalId(), id, f.metadata()));
    }

    private Feature fromJts(Optional<String> id, Geometry geometry, Map<String, Object> attributes) {
        return Feature.of(
                id,
                Optional.of(Utils.fromJts(geometryTransformer.transform(geometry))),
                attributes
        );
    }

    private static Map<String, Object> convertMetadata(String externalId, String id, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap();
        metadata.entrySet().stream()
                        .filter(entry -> !(entry.getValue() instanceof ComplexAttribute))
                        .forEach(entry -> output.put(entry.getKey(), entry.getValue()));
        output.put("_id", id);  // TODO: eliminate the _id concept
        output.put("_externalId", externalId);
        return output;
    }
}
