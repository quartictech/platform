package io.quartic.weyl.websocket;

import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Emitter.BackpressureMode;
import rx.Observable;

import java.util.*;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static rx.Observable.fromEmitter;

public class GeofenceStatusHandler implements ClientStatusMessageHandler {
    private final GeofenceStore geofenceStore;
    private final LayerStore layerStore;
    private final FeatureConverter featureConverter;

    public GeofenceStatusHandler(GeofenceStore geofenceStore, LayerStore layerStore, FeatureConverter featureConverter) {
        this.geofenceStore = geofenceStore;
        this.layerStore = layerStore;
        this.featureConverter = featureConverter;
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return clientStatus
                .map(ClientStatusMessage::geofence)
                .distinctUntilChanged()
                .doOnNext(this::handleMessage)
                .switchMap(x -> upstream());    // TODO: this is an utterly gross hack
    }

    private void handleMessage(GeofenceStatus geofence) {
        geofence.features().ifPresent(f -> updateStore(geofence.type(), geofence.bufferDistance(), featuresFrom(f)));
        geofence.layerId().ifPresent(id -> updateStore(geofence.type(), geofence.bufferDistance(), featuresFrom(id)));
    }

    private Stream<Feature> featuresFrom(FeatureCollection features) {
        return featureConverter.toModel(features)
                .stream()
                .map(f -> FeatureImpl.of(
                        EntityIdImpl.of("custom"),
                        f.geometry(),
                        f.attributes()
                ));
    }

    private Stream<Feature> featuresFrom(LayerId layerId) {
        // TODO: validate that layer exists
        return layerStore.getLayer(layerId).get()
                .features().stream();
    }

    private void updateStore(GeofenceType type, double bufferDistance, Stream<Feature> features) {
        final List<Geofence> geofences = features
                .map(f -> FeatureImpl.copyOf(f)
                        .withAttributes(AttributesImpl.of(singletonMap(ALERT_LEVEL, alertLevel(f))))
                        .withGeometry(bufferOp(f.geometry(), bufferDistance))
                        .withEntityId(EntityIdImpl.of("geofence/" + f.entityId().uid()))
                )
                .filter(f -> !f.geometry().isEmpty())
                .map(f -> GeofenceImpl.of(type, f))
                .collect(toList());
        geofenceStore.setGeofences(geofences);
    }

    private Alert.Level alertLevel(Feature feature) {
        final Object level = feature.attributes().attributes().get(ALERT_LEVEL);
        try {
            return Alert.Level.valueOf(level.toString().toUpperCase());
        } catch (Exception e) {
            return Alert.Level.SEVERE;    // Default
        }
    }

    private Observable<SocketMessage> upstream() {
        return fromEmitter(
                emitter -> {
                    final GeofenceListener listener = new GeofenceListener() {
                        final Set<Violation> violations = newLinkedHashSet();

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
                            emitter.onNext(GeofenceGeometryUpdateMessageImpl.of(
                                    featureConverter.toGeojson(features)
                            ));
                        }

                        private void sendViolationsUpdate() {
                            emitter.onNext(GeofenceViolationsUpdateMessageImpl.of(
                                    violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList())
                            ));
                        }
                    };

                    geofenceStore.addListener(listener);
                    emitter.setCancellation(() -> geofenceStore.removeListener(listener));
                },
                BackpressureMode.BUFFER
        );
    }
}
