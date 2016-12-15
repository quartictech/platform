package io.quartic.weyl.websocket;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geofence.ViolationImpl;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.alert.Alert.Level.SEVERE;
import static io.quartic.weyl.core.alert.Alert.Level.WARNING;
import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.from;

public class GeofenceStatusHandlerShould {
    private final Attributes featureAttributes = mock(Attributes.class);
    private final NakedFeature featureA = NakedFeatureImpl.of(Optional.empty(), polygon(5.0), featureAttributes);
    private final NakedFeature featureB = NakedFeatureImpl.of(Optional.empty(), polygon(6.0), featureAttributes);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new GeofenceStatusHandler(geofenceStore, snapshotSequences, converter);

    @Test
    public void send_geometry_update() throws Exception {
        final List<Feature> features = mock(List.class);
        final FeatureCollection featureCollection = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
        when(converter.toGeojson(any())).thenReturn(featureCollection);
        onListen(listener -> listener.onGeometryChange(features));

        TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));

        sub.awaitValueCount(1, 100, MILLISECONDS);
        verify(converter).toGeojson(features);
        assertThat(sub.getOnNextEvents(), contains(GeofenceGeometryUpdateMessageImpl.of(featureCollection)));
    }

    @Test
    public void send_violation_update_accounting_for_cumulative_changes() throws Exception {
        final EntityId geofenceIdA = mock(EntityId.class);
        final EntityId geofenceIdB = mock(EntityId.class);
        final Violation violationA = violation(geofenceIdA);
        final Violation violationB = violation(geofenceIdB);
        onListen(listener -> {
            listener.onViolationBegin(violationA);
            listener.onViolationBegin(violationB);
            listener.onViolationEnd(violationA);
        });

        TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));

        sub.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), contains(
                GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA), 0, 0, 1),
                GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA, geofenceIdB), 0, 0, 2),
                GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdB), 0, 0, 1)
        ));
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));

        subscribeToHandler(status(builder -> builder.features(features)));

        verify(converter).toModel(features);
        verifyGeofence("custom", featureA, featureB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        final LayerId layerId = mock(LayerId.class);
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);
        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenReturn(
                newArrayList(
                        modelFeatureOf(featureA),
                        modelFeatureOf(featureB)
                ).stream()
        );

        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        verifyGeofence("xyz", featureA, featureB);
    }

    @Test
    public void set_empty_geofence_when_disabled() throws Exception {
        subscribeToHandler(status(builder -> builder.enabled(false)));

        verifyGeofence("");
    }

    @Test
    public void set_level_attribute_based_on_attribute_from_features() throws Exception {
        when(featureAttributes.attributes()).thenReturn(singletonMap(ALERT_LEVEL, "warning"));
        when(converter.toModel(any())).thenReturn(newArrayList(featureA));

        subscribeToHandler(status(builder -> builder.features(mock(FeatureCollection.class))));

        verifyGeofence("custom", WARNING, featureA);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));

        subscribeToHandler(status(builder -> builder.features(features)));

        verifyGeofence("custom", featureA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));

        subscribeToHandler(status(builder -> builder.features(features).bufferDistance(1.0)));

        verifyGeofence("custom", NakedFeatureImpl.of(Optional.empty(), bufferOp(point(), 1.0), featureAttributes));
    }

    @Test
    public void unsubscribe_from_store_on_downstream_unsubscribe() throws Exception {
        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(builder -> builder.bufferDistance(1.0)));
        sub.unsubscribe();

        verify(geofenceStore).removeListener(any());
    }

    @Test
    public void ignore_status_changes_not_involving_geofence_change() throws Exception {
        final ClientStatusMessage statusA = status(identity());
        final ClientStatusMessage statusB = status(identity());
        when(statusA.subscribedLiveLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));
        when(statusB.subscribedLiveLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));

        subscribeToHandler(statusA, statusB);

        verify(geofenceStore, never()).removeListener(any());
    }

    private ClientStatusMessage status(Function<GeofenceStatusImpl.Builder, GeofenceStatusImpl.Builder> builderHacks) {
        GeofenceStatusImpl.Builder builder = GeofenceStatusImpl.builder()
                .enabled(true)
                .type(GeofenceType.INCLUDE)
                .features(Optional.empty())
                .defaultLevel(SEVERE)
                .bufferDistance(0.0);
        builder = builderHacks.apply(builder);
        ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.geofence()).thenReturn(builder.build());
        return msg;
    }

    private TestSubscriber<SocketMessage> subscribeToHandler(ClientStatusMessage... statuses) {
        TestSubscriber<SocketMessage> sub = TestSubscriber.create();
        from(statuses).compose(handler).subscribe(sub);
        return sub;
    }

    private void verifyGeofence(String id, NakedFeature... features) {
        verifyGeofence(id, SEVERE, features);
    }

    private void verifyGeofence(String id, Alert.Level level, NakedFeature... features) {
        verify(geofenceStore).setGeofences(
                stream(features)
                        .map(p -> geofenceOf(id, level, p.geometry()))
                        .collect(toList()));
    }

    private Geofence geofenceOf(String id, Alert.Level level, Geometry geometry) {
        return GeofenceImpl.of(
                GeofenceType.INCLUDE,
                io.quartic.weyl.core.model.FeatureImpl.of(
                        EntityIdImpl.of("geofence/" + id),
                        geometry,
                        AttributesImpl.of(singletonMap(ALERT_LEVEL, level))
                )
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
}
