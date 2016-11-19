package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.UpdateMessageGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

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
import static io.quartic.weyl.core.source.ConversionUtils.convertFromModelAttributes;
import static java.util.stream.Collectors.toList;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener, GeofenceListener {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateServer.class);
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;
    private final Set<Violation> violations = newLinkedHashSet();
    private final List<LayerSubscription> subscriptions = newArrayList();
    private final Collection<UpdateMessageGenerator> generators;
    private final GeofenceStore geofenceStore;
    private final AlertProcessor alertProcessor;
    private final LayerStore layerStore;
    private Session session;
    private final PublishSubject<Collection<EntityId>> subscribedEntityIds;
    private final Multiplexer<EntityId, AbstractFeature> mux;
    private List<Subscription> generatorSubscriptions;

    public UpdateServer(
            LayerStore layerStore,
            Multiplexer<EntityId, AbstractFeature> mux,
            Collection<UpdateMessageGenerator> generators,
            GeofenceStore geofenceStore,
            AlertProcessor alertProcessor,
            GeometryTransformer geometryTransformer,
            ObjectMapper objectMapper
    ) {
        this.layerStore = layerStore;
        this.generators = ImmutableList.copyOf(generators);
        this.geofenceStore = geofenceStore;
        this.alertProcessor = alertProcessor;
        this.geometryTransformer = geometryTransformer;
        this.objectMapper = objectMapper;
        this.subscribedEntityIds = PublishSubject.create();
        this.mux = mux;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        this.session = session;
        final Observable<List<AbstractFeature>> muxed = mux.multiplex(subscribedEntityIds).share();
        this.generatorSubscriptions = generators.stream()
                .map(g -> muxed.subscribe(entities -> sendMessage(g.generate(entities))))
                .collect(toList());
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
                        session.getId(), stringify(csm.subscribedLiveLayerIds()), stringify(csm.subscribedEntityIds()));
                unsubscribeAll();
                csm.subscribedLiveLayerIds().forEach(this::subscribe);
                subscribedEntityIds.onNext(csm.subscribedEntityIds());
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
        generatorSubscriptions.forEach(Subscription::unsubscribe);
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
    public void onGeometryChange(Collection<AbstractFeature> features) {
        sendMessage(GeofenceGeometryUpdateMessage.of(fromJts(features)));
    }

    private void unsubscribeAll() {
        subscriptions.forEach(layerStore::removeSubscriber);
        subscriptions.clear();
    }

    private void subscribe(LayerId layerId) {
        subscriptions.add(layerStore.addSubscriber(layerId, state -> sendLayerUpdate(layerId, state)));
    }

    private void sendViolationsUpdate() {
        sendMessage(GeofenceViolationsUpdateMessage.of(violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList())));
    }

    private void sendLayerUpdate(LayerId layerId, LayerState state) {
        sendMessage(LayerUpdateMessage.builder()
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
            e.printStackTrace();    // TODO
        }
    }

    private FeatureCollection fromJts(Collection<AbstractFeature> features) {
        return FeatureCollection.of(
                features.stream()
                        .map(this::fromJts)
                        .collect(toList())
        );
    }

    private Feature fromJts(AbstractFeature f) {
        return Feature.of(
                Optional.empty(),
                Optional.of(Utils.fromJts(geometryTransformer.transform(f.geometry()))),
                convertFromModelAttributes(f)
        );
    }
}
