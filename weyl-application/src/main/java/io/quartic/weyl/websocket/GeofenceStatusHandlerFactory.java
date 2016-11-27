package io.quartic.weyl.websocket;

import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.stream.Collectors.toList;

public class GeofenceStatusHandlerFactory implements ClientStatusMessageHandler.Factory {
    private final GeofenceStore geofenceStore;
    private final FeatureConverter featureConverter;

    public GeofenceStatusHandlerFactory(GeofenceStore geofenceStore, FeatureConverter featureConverter) {
        this.geofenceStore = geofenceStore;
        this.featureConverter = featureConverter;
    }

    @Override
    public ClientStatusMessageHandler create(Consumer<SocketMessage> messageConsumer) {
        final GeofenceListener listener = createListener(messageConsumer);
        geofenceStore.addListener(listener);

        return new ClientStatusMessageHandler() {
            @Override
            public void onClientStatusMessage(ClientStatusMessage msg) {
                // TODO
            }

            @Override
            public void close() throws Exception {
                geofenceStore.removeListener(listener);
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
