package io.quartic.weyl.websocket;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.websocket.message.*;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GeofenceStatusHandlerFactoryShould {
    private static final Attributes FEATURE_ATTRIBUTES = mock(Attributes.class);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final Consumer<SocketMessage> messageConsumer = mock(Consumer.class);
    private final NakedFeature featureA = NakedFeatureImpl.of(Optional.empty(), polygon(5.0), FEATURE_ATTRIBUTES);
    private final NakedFeature featureB = NakedFeatureImpl.of(Optional.empty(), polygon(6.0), FEATURE_ATTRIBUTES);

    @Test
    public void send_geofence_geometry_update() throws Exception {
        final List<Feature> features = mock(List.class);
        final FeatureCollection featureCollection = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
        when(converter.toGeojson(any())).thenReturn(featureCollection);
        onListen(listener -> listener.onGeometryChange(features));

        createHandler();

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

        createHandler();

        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA)));
        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA, geofenceIdB)));
        verify(messageConsumer).accept(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdB)));
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));

        createHandler().handle(messageOf(GeofenceStatusImpl.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build()));

        verify(converter).toModel(features);
        verifyGeofence("custom", featureA, featureB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        final LayerId layerId = LayerIdImpl.of("789");
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);
        when(layerStore.getLayer(layerId)).thenReturn(Optional.of(layer));
        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenReturn(
                newArrayList(
                        modelFeatureOf(featureA),
                        modelFeatureOf(featureB)
                ).stream()
        );

        createHandler().handle(messageOf(GeofenceStatusImpl.builder()
                .type(GeofenceType.INCLUDE)
                .layerId(layerId)
                .bufferDistance(0.0)
                .build()));

        verifyGeofence("xyz", featureA, featureB);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                NakedFeatureImpl.of(Optional.empty(), point(), FEATURE_ATTRIBUTES)
        ));

        createHandler().handle(messageOf(GeofenceStatusImpl.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build()));

        verifyGeofence("custom", featureA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                NakedFeatureImpl.of(Optional.empty(), point(), FEATURE_ATTRIBUTES)
        ));

        createHandler().handle(messageOf(GeofenceStatusImpl.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(1.0)
                .build()));

        verifyGeofence("custom", NakedFeatureImpl.of(Optional.empty(), bufferOp(point(), 1.0), FEATURE_ATTRIBUTES));
    }

    private ClientStatusMessage messageOf(GeofenceStatus geofenceStatus) {
        ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.geofence()).thenReturn(geofenceStatus);
        return msg;
    }

    private void verifyGeofence(String id, NakedFeature... features) {
        verify(geofenceStore).setGeofences(
                stream(features)
                        .map(p -> geofenceOf(id, p))
                        .collect(toList()));
    }

    private Geofence geofenceOf(String id, NakedFeature feature) {
        return GeofenceImpl.of(
                GeofenceType.INCLUDE,
                io.quartic.weyl.core.model.FeatureImpl.of(EntityIdImpl.of("geofence/" + id), feature.geometry(), feature.attributes())
        );
    }

    private Feature modelFeatureOf(NakedFeature feature) {
        return io.quartic.weyl.core.model.FeatureImpl.of(
                EntityIdImpl.of("xyz"),
                feature.geometry(),
                feature.attributes());
    }

    private Geometry point() {
        return new GeometryFactory().createPoint(new Coordinate(1.0, 2.0));
    }

    private Geometry polygon(double offset) {
        return new GeometryFactory().createPolygon(new Coordinate[] {
                new Coordinate(1.0 + offset, 2.0 + offset),
                new Coordinate(1.0 + offset, 3.0 + offset),
                new Coordinate(2.0 + offset, 3.0 + offset),
                new Coordinate(2.0 + offset, 2.0 + offset),
                new Coordinate(1.0 + offset, 2.0 + offset)
        });
    }

    private void onListen(Consumer<GeofenceListener> consumer) {
        doAnswer(invocation -> {
            final GeofenceListener listener = invocation.getArgument(0);
            consumer.accept(listener);
            return null;
        }).when(geofenceStore).addListener(any());
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

    private ClientStatusMessageHandler createHandler() {
        return new GeofenceStatusHandlerFactory(geofenceStore, layerStore, converter).create(messageConsumer);
    }
}
