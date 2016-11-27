package io.quartic.weyl.websocket;

import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.*;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static java.util.stream.Collectors.toList;

public class GeofenceStatusHandlerFactory implements ClientStatusMessageHandler.Factory {
    private final GeofenceStore geofenceStore;
    private final LayerStore layerStore;
    private final FeatureConverter featureConverter;

    public GeofenceStatusHandlerFactory(GeofenceStore geofenceStore, LayerStore layerStore, FeatureConverter featureConverter) {
        this.geofenceStore = geofenceStore;
        this.layerStore = layerStore;
        this.featureConverter = featureConverter;
    }

    @Override
    public ClientStatusMessageHandler create(Consumer<SocketMessage> messageConsumer) {
        final GeofenceListener listener = createListener(messageConsumer);
        geofenceStore.addListener(listener);

        return new ClientStatusMessageHandler() {
            @Override
            public void handle(ClientStatusMessage msg) {
                msg.geofence().features().ifPresent(f -> updateStore(msg.geofence().type(), msg.geofence().bufferDistance(), featuresFrom(f)));
                msg.geofence().layerId().ifPresent(id -> updateStore(msg.geofence().type(), msg.geofence().bufferDistance(), featuresFrom(id)));
            }

            @Override
            public void close() throws Exception {
                geofenceStore.removeListener(listener);
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
                                .withGeometry(bufferOp(f.geometry(), bufferDistance))
                                .withEntityId(EntityIdImpl.of("geofence/" + f.entityId().uid()))
                        )
                        .filter(f -> !f.geometry().isEmpty())
                        .map(f -> GeofenceImpl.of(type, f))
                        .collect(toList());
                geofenceStore.setGeofences(geofences);
            }
        };
    }


    private GeofenceListener createListener(final Consumer<SocketMessage> messageConsumer) {
        final Set<Violation> violations = newLinkedHashSet();

        return new GeofenceListener() {
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
                messageConsumer.accept(GeofenceGeometryUpdateMessageImpl.of(
                        featureConverter.toGeojson(features)
                ));
            }

            private void sendViolationsUpdate() {
                messageConsumer.accept(GeofenceViolationsUpdateMessageImpl.of(
                        violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList())
                ));
            }
        };
    }
}
