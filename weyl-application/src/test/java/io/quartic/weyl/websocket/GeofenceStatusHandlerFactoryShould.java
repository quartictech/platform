package io.quartic.weyl.websocket;

import com.google.common.collect.ImmutableList;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.*;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GeofenceStatusHandlerFactoryShould {
    private final GeofenceStore store = mock(GeofenceStore.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final Consumer<SocketMessage> messageConsumer = mock(Consumer.class);


    @Test
    public void send_geofence_geometry_update() throws Exception {
        final List<Feature> features = mock(List.class);
        final FeatureCollection featureCollection = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
        when(converter.toGeojson(any())).thenReturn(featureCollection);
        onListen(listener -> listener.onGeometryChange(features));

        new GeofenceStatusHandlerFactory(store, converter).create(messageConsumer);

        verify(converter).toGeojson(features);
        verify(messageConsumer).accept(GeofenceGeometryUpdateMessageImpl.of(featureCollection));
    }

    @Test
    public void send_geofence_violation_update_accounting_for_cumulative_changes() throws Exception {
        final EntityId geofenceIdA = EntityIdImpl.of("37");
        final EntityId geofenceIdB = EntityIdImpl.of("38");
        final Violation violationA = violation(geofenceIdA);
        final Violation violationB = violation(geofenceIdB);
        onListen(listener -> {
            listener.onViolationBegin(violationA);
            listener.onViolationBegin(violationB);
            listener.onViolationEnd(violationA);
        });

        new GeofenceStatusHandlerFactory(store, converter).create(messageConsumer);

        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA)));
        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA, geofenceIdB)));
        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdB)));
    }

    private void onListen(Consumer<GeofenceListener> consumer) {
        doAnswer(invocation -> {
            final GeofenceListener listener = invocation.getArgument(0);
            consumer.accept(listener);
            return null;
        }).when(store).addListener(any());
    }

    private Violation violation(EntityId geofenceId) {
        final Geofence geofence = mock(Geofence.class, RETURNS_DEEP_STUBS);
        when(geofence.feature().entityId()).thenReturn(geofenceId);
        return ViolationImpl.of(
                mock(Feature.class),
                geofence,
                "Hmmm"
        );
    }
}
