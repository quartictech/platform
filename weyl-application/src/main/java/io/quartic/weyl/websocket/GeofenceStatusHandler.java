package io.quartic.weyl.websocket;

import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Emitter.BackpressureMode;
import rx.Observable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.alert.Alert.Level.INFO;
import static io.quartic.weyl.core.alert.Alert.Level.SEVERE;
import static io.quartic.weyl.core.alert.Alert.Level.WARNING;
import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
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

    private void handleMessage(GeofenceStatus status) {
        if (status.enabled()) {
            status.features().ifPresent(f -> updateStore(status, featuresFrom(f)));
            status.layerId().ifPresent(id -> updateStore(status, featuresFrom(id)));
        } else {
            updateStore(status, Collections.<Feature>emptyList().stream());
        }
    }

    private Stream<Feature> featuresFrom(FeatureCollection features) {
        return featureConverter.toModel(features)
                .stream()
                .map(this::annotateFeature);
    }

    private Feature annotateFeature(NakedFeature feature) {
        return FeatureImpl.of(
                EntityIdImpl.of("custom"),
                feature.geometry(),
                feature.attributes()
        );
    }

    private Stream<Feature> featuresFrom(LayerId layerId) {
        // TODO: validate that layer exists
        return layerStore.layer(layerId)
                .first()
                .toBlocking()
                .single()
                .features()
                .stream();
    }

    private void updateStore(GeofenceStatus status, Stream<Feature> features) {
        final List<Geofence> geofences = features
                .map(f -> FeatureImpl.copyOf(f)
                        .withAttributes(AttributesImpl.of(singletonMap(ALERT_LEVEL, alertLevel(f, status.defaultLevel()))))
                        .withGeometry(bufferOp(f.geometry(), status.bufferDistance()))
                        .withEntityId(EntityIdImpl.of("geofence/" + f.entityId().uid()))
                )
                .filter(f -> !f.geometry().isEmpty())
                .map(f -> GeofenceImpl.of(status.type(), f))
                .collect(toList());
        geofenceStore.setGeofences(geofences);
    }

    private Observable<SocketMessage> upstream() {
        return fromEmitter(
                emitter -> {
                    final GeofenceListener listener = new GeofenceListener() {
                        final Set<Violation> violations = newLinkedHashSet();
                        final Map<Alert.Level, Integer> counts = newHashMap();

                        @Override
                        public void onViolationBegin(Violation violation) {
                            synchronized (violations) {
                                final Alert.Level level = alertLevel(violation.geofence().feature());
                                violations.add(violation);
                                counts.put(level, counts.getOrDefault(level, 0) + 1);
                                sendViolationsUpdate();
                            }
                        }

                        @Override
                        public void onViolationEnd(Violation violation) {
                            synchronized (violations) {
                                final Alert.Level level = alertLevel(violation.geofence().feature());
                                violations.remove(violation);
                                counts.put(level, counts.getOrDefault(level, 1) - 1);   // Prevents going below 0 in cases that should never happen
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
                                    violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList()),
                                    counts.getOrDefault(INFO, 0),
                                    counts.getOrDefault(WARNING, 0),
                                    counts.getOrDefault(SEVERE, 0)
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
