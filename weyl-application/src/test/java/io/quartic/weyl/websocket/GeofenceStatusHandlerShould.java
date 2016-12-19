package io.quartic.weyl.websocket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.rx.StateAndOutputImpl;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.Output;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.State;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.AlertImpl;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.model.SnapshotImpl;
import io.quartic.weyl.websocket.message.AlertMessage;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.ReplaySubject;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.weyl.core.geofence.Geofence.ALERT_LEVEL;
import static io.quartic.weyl.core.model.Alert.Level.INFO;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Alert.Level.WARNING;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.from;
import static rx.Observable.just;

public class GeofenceStatusHandlerShould {
    private final Attributes featureAttributes = mock(Attributes.class);
    private final NakedFeature featureA = NakedFeatureImpl.of(Optional.empty(), polygon(5.0), featureAttributes);
    private final NakedFeature featureB = NakedFeatureImpl.of(Optional.empty(), polygon(6.0), featureAttributes);
    private final FeatureCollection featureCollection = mock(FeatureCollection.class);

    private final LayerId layerId = mock(LayerId.class);
    private final LayerSpec layerSpec = mock(LayerSpec.class);

    private final GeofenceViolationDetector detector = mock(GeofenceViolationDetector.class);

    private final Interceptor<LayerSnapshotSequence> interceptor = Interceptor.create();
    private final ReplaySubject<LayerSnapshotSequence> snapshotSequences = ReplaySubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new GeofenceStatusHandler(
            detector,
            snapshotSequences.compose(interceptor),
            converter
    );

    @Before
    public void before() throws Exception {
        when(layerSpec.id()).thenReturn(layerId);

        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));
        when(converter.toGeojson(any())).thenReturn(featureCollection);

        // Default behaviour
        when(detector.create(any())).thenReturn(mock(State.class));
        when(detector.next(any(), any())).thenReturn(StateAndOutputImpl.of(mock(State.class), mock(Output.class)));
    }

    // TODO: handling of layer completion

    @Test
    public void send_violation_update_when_changed() throws Exception {
        mockDetectorBehaviour(false, true);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), emptyList()));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), contains(
                GeofenceViolationsUpdateMessageImpl.of(newArrayList(EntityId.fromString("Pluto")), 1, 2, 3)
        ));
    }

    @Test
    public void not_send_violation_update_when_not_changed() throws Exception {
        mockDetectorBehaviour(false, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), emptyList()));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), empty());
    }

    @Test
    public void send_alert_for_new_violation() throws Exception {
        mockDetectorBehaviour(true, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), emptyList()));

        assertThat(extractByType(sub, AlertMessage.class), contains(
                AlertMessageImpl.of(AlertImpl.of(
                        "Geofence violation",
                        Optional.of("Boundary violated by entity 'Goofy'"),
                        SEVERE
                ))
        ));
    }

    @Test
    public void maintain_detector_state_between_calls() throws Exception {
        final State state = mock(State.class);
        when(detector.create(any())).thenReturn(state);

        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), emptyList()));

        verify(detector).next(eq(state), any());
    }

    @Test
    public void call_detector_with_layer_diff() throws Exception {
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), diff));

        verify(detector).next(any(), eq(diff));
    }

    @Test
    public void ignore_non_live_layer_changes() throws Exception {
        when(layerSpec.indexable()).thenReturn(true);
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(layer(), diff));

        verify(detector, never()).next(any(), any());
    }

    private void mockDetectorBehaviour(boolean newViolations, boolean hasChanged) {
        final Violation violation = mock(Violation.class);
        when(violation.entityId()).thenReturn(EntityId.fromString("Goofy"));
        when(violation.geofenceId()).thenReturn(EntityId.fromString("Pluto"));
        when(violation.level()).thenReturn(SEVERE);

        final Output output = mock(Output.class);
        when(output.hasChanged()).thenReturn(hasChanged);
        if (newViolations) {
            when(output.newViolations()).thenReturn(newArrayList(violation));
        } else {
            when(output.violations()).thenReturn(newHashSet(violation));
        }

        when(output.counts()).thenReturn(map(
                entry(INFO, 1),
                entry(WARNING, 2),
                entry(SEVERE, 3)
        ));

        when(detector.next(any(), any())).thenReturn(StateAndOutputImpl.of(mock(State.class), output));
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        subscribeToHandler(status(builder -> builder.features(featureCollection)));

        verify(converter, atLeastOnce()).toModel(featureCollection);
        verifyGeofences("custom", featureA, featureB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        snapshotSequences.onNext(sequence(layer(), emptyList()));
        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        verifyGeofences("xyz", featureA, featureB);
    }

    @Test
    public void send_geometry_update_when_geofence_changes() throws Exception {
        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(builder -> builder.features(featureCollection)));

        assertThat(extractByType(sub, GeofenceGeometryUpdateMessage.class),
                contains(GeofenceGeometryUpdateMessageImpl.of(featureCollection)));
    }

    @Test
    public void set_empty_geofence_when_disabled() throws Exception {
        subscribeToHandler(status(builder -> builder.enabled(false)));

        verifyGeofences("");
    }

    @Test
    public void set_level_attribute_based_on_attribute_from_features() throws Exception {
        when(featureAttributes.attributes()).thenReturn(singletonMap(ALERT_LEVEL, "warning"));

        subscribeToHandler(status(builder -> builder.features(mock(FeatureCollection.class))));

        verifyGeofences("custom", WARNING, featureA, featureB);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));

        subscribeToHandler(status(builder -> builder.features(features)));

        verifyGeofences("custom", featureA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));

        subscribeToHandler(status(builder -> builder.features(features).bufferDistance(1.0)));

        verifyGeofences("custom", NakedFeatureImpl.of(Optional.empty(), bufferOp(point(), 1.0), featureAttributes));
    }

    @Test
    public void ignore_status_changes_not_involving_geofence_change() throws Exception {
        final ClientStatusMessage statusA = status(identity());
        final ClientStatusMessage statusB = status(identity());
        when(statusA.openLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));
        when(statusB.openLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));

        subscribeToHandler(statusA, statusB);

        verify(detector, times(1)).create(any());
    }

    // This covers the non-reactive hackery we still have in the implementation
    @Test
    public void unsubscribe_from_snapshots_on_unsubscribe() throws Exception {
        subscribeToHandler(status(identity())).unsubscribe();

        assertThat(interceptor.unsubscribed(), equalTo(true));
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

    @SuppressWarnings("unchecked")
    private <T extends SocketMessage> List<T> extractByType(TestSubscriber<SocketMessage> sub, Class<T> clazz) {
        return sub.getOnNextEvents().stream()
                .filter(msg -> clazz.isAssignableFrom(msg.getClass()))
                .map(msg -> (T) msg)
                .collect(toList());
    }

    private TestSubscriber<SocketMessage> subscribeToHandler(ClientStatusMessage... statuses) {
        TestSubscriber<SocketMessage> sub = TestSubscriber.create();
        from(statuses).compose(handler).subscribe(sub);
        return sub;
    }

    private void verifyGeofences(String id, NakedFeature... features) {
        verifyGeofences(id, SEVERE, features);
    }

    private void verifyGeofences(String id, Alert.Level level, NakedFeature... features) {
        verify(detector).create(
                stream(features).map(p -> geofenceOf(id, level, p.geometry())).collect(toList())
        );
    }

    private LayerSnapshotSequence sequence(Layer layer, List<Feature> diff) {
        return LayerSnapshotSequenceImpl.of(layerSpec, just(SnapshotImpl.of(layer, diff)));
    }

    private Layer layer() {
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);

        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenAnswer(invocation -> newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB)).stream());
        return layer;
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
}
