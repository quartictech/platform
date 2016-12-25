package io.quartic.weyl.websocket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.rx.StateAndOutput;
import io.quartic.common.geojson.FeatureCollection;
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
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
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
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

import java.util.Collection;
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
    private final NakedFeature featureA = nakedFeature(polygon(5.0));
    private final NakedFeature featureB = nakedFeature(polygon(6.0));
    private final FeatureCollection featureCollection = mock(FeatureCollection.class);

    private final LayerId layerId = mock(LayerId.class);
    private final LayerSpec layerSpec = mock(LayerSpec.class);

    private final GeofenceViolationDetector detector = mock(GeofenceViolationDetector.class);

    private final ReplaySubject<LayerSnapshotSequence> snapshotSequences = ReplaySubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new GeofenceStatusHandler(snapshotSequences, detector, converter);

    @Before
    public void before() throws Exception {
        when(layerSpec.id()).thenReturn(layerId);

        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));
        when(converter.toGeojson(any())).thenReturn(featureCollection);

        // Default behaviour
        when(detector.create(any())).thenReturn(mock(State.class));
        when(detector.next(any(), any())).thenReturn(new StateAndOutput<>(mock(State.class), mock(Output.class)));
    }

    @Test
    public void send_violation_update_when_changed() throws Exception {
        mockDetectorBehaviour(false, true);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), contains(
                GeofenceViolationsUpdateMessageImpl.of(newArrayList(EntityId.fromString("Pluto")), 1, 2, 3)
        ));
    }

    @Test
    public void not_send_violation_update_when_not_changed() throws Exception {
        mockDetectorBehaviour(false, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), empty());
    }

    @Test
    public void send_alert_for_new_violation() throws Exception {
        mockDetectorBehaviour(true, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

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
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        verify(detector).next(eq(state), any());
    }

    @Test
    public void call_detector_with_layer_diff() throws Exception {
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(just(snapshot(layer(), diff))));

        verify(detector).next(any(), eq(diff));
    }

    @Test
    public void ignore_non_live_layer_changes() throws Exception {
        when(layerSpec.indexable()).thenReturn(true);
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(identity()));
        snapshotSequences.onNext(sequence(just(snapshot(layer(), diff))));

        verify(detector, never()).next(any(), any());
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        subscribeToHandler(status(builder -> builder.features(featureCollection)));

        verify(converter, atLeastOnce()).toModel(featureCollection);
        verifyGeofences("custom", newArrayList(featureA, featureB));
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));
        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        verifyGeofences("xyz", newArrayList(featureA, featureB));
    }

    @Test
    public void update_geofence_when_layer_updates() throws Exception {
        final NakedFeature featureC = nakedFeature(polygon(7.0));
        final NakedFeature featureD = nakedFeature(polygon(8.0));

        final PublishSubject<Snapshot> snapshots = PublishSubject.create();
        snapshotSequences.onNext(sequence(snapshots));

        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        snapshots.onNext(snapshot(layer()));
        snapshots.onNext(snapshot(layer(modelFeatureOf(featureC), modelFeatureOf(featureD))));

        verifyGeofences("xyz", newArrayList(featureA, featureB), newArrayList(featureC, featureD));
    }

    @Test
    public void set_empty_geofence_when_layer_empty() throws Exception {
        snapshotSequences.onNext(sequence(just(snapshot(layer(new Feature[] {})))));    // Empty!
        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        verifyGeofences("", emptyList());
    }

    @Test
    public void set_empty_geofence_when_no_features_or_layer() throws Exception {
        subscribeToHandler(status(identity()));

        verifyGeofences("", emptyList());
    }

    @Test
    public void set_empty_geofence_when_layer_missing() throws Exception {
        // Nothing added to snapshotSequences!
        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        verifyGeofences("", emptyList());
    }

    @Test
    public void send_geometry_update_when_geofence_changes() throws Exception {
        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(builder -> builder.features(featureCollection)));

        assertThat(extractByType(sub, GeofenceGeometryUpdateMessage.class),
                contains(GeofenceGeometryUpdateMessageImpl.of(featureCollection)));
    }

    @Test
    public void set_level_attribute_based_on_attribute_from_features() throws Exception {
        when(featureAttributes.attributes()).thenReturn(singletonMap(ALERT_LEVEL, "warning"));

        subscribeToHandler(status(builder -> builder.features(mock(FeatureCollection.class))));

        verifyGeofences("custom", WARNING, newArrayList(featureA, featureB));
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                nakedFeature(point())
        ));

        subscribeToHandler(status(builder -> builder.features(features)));

        verifyGeofences("custom", newArrayList(featureA));
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                nakedFeature(point())
        ));

        subscribeToHandler(status(builder -> builder.features(features).bufferDistance(1.0)));

        verifyGeofences("custom", newArrayList(nakedFeature(bufferOp(point(), 1.0))));
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

    private ClientStatusMessage status(Function<GeofenceStatusImpl.Builder, GeofenceStatusImpl.Builder> builderHacks) {
        GeofenceStatusImpl.Builder builder = GeofenceStatusImpl.builder()
                .type(GeofenceType.INCLUDE)
                .features(Optional.empty())
                .defaultLevel(SEVERE)
                .bufferDistance(0.0);
        builder = builderHacks.apply(builder);
        ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.geofence()).thenReturn(builder.build());
        return msg;
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

        when(detector.next(any(), any())).thenReturn(new StateAndOutput<>(mock(State.class), output));
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

    @SafeVarargs
    private final void verifyGeofences(String id, Collection<NakedFeature>... features) {
        verifyGeofences(id, SEVERE, features);
    }

    @SafeVarargs
    private final void verifyGeofences(String id, Alert.Level level, Collection<NakedFeature>... features) {
        verify(detector).create(
                features[0].stream().map(p -> geofenceOf(id, level, p.geometry())).collect(toList())
        );
    }

    private LayerSnapshotSequence sequence(Observable<Snapshot> snapshots) {
        return LayerSnapshotSequenceImpl.of(layerSpec, snapshots);
    }

    private Snapshot snapshot(Layer layer) {
        return SnapshotImpl.of(layer, emptyList());
    }

    private Snapshot snapshot(Layer layer, List<Feature> diff) {
        return SnapshotImpl.of(layer, diff);
    }

    private Layer layer() {
        return layer(modelFeatureOf(featureA), modelFeatureOf(featureB));
    }

    private Layer layer(Feature... features) {
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);

        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenAnswer(invocation -> stream(features));
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

    private NakedFeature nakedFeature(Geometry polygon) {
        return NakedFeatureImpl.of(Optional.empty(), polygon, featureAttributes);
    }

}
