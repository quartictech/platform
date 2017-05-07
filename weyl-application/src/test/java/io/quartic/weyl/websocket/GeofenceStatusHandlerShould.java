package io.quartic.weyl.websocket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.rx.StateAndOutput;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.Output;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.State;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.websocket.message.AlertMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.weyl.core.feature.FeatureConverter.MINIMAL_MANIPULATOR;
import static io.quartic.weyl.core.geofence.GeofenceType.INCLUDE;
import static io.quartic.weyl.core.model.Alert.Level.INFO;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Alert.Level.WARNING;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
    private final FeatureConverter converter = mock(FeatureConverter.class, RETURNS_DEEP_STUBS);
    private final ClientStatusMessageHandler handler = new GeofenceStatusHandler(snapshotSequences, detector, converter);

    @Before
    public void before() throws Exception {
        when(layerSpec.getId()).thenReturn(layerId);

        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));
        when(converter.toGeojson(eq(MINIMAL_MANIPULATOR), any(Collection.class))).thenReturn(featureCollection);

        // Default behaviour
        when(detector.create(any())).thenReturn(mock(State.class));
        when(detector.next(any(), any())).thenReturn(new StateAndOutput<>(mock(State.class), mock(Output.class)));
    }

    @Test
    public void send_violation_update_when_changed() throws Exception {
        mockDetectorBehaviour(false, true);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), contains(
                new GeofenceViolationsUpdateMessage(newHashSet(new EntityId("Pluto")), 1, 2, 3)
        ));
    }

    @Test
    public void not_send_violation_update_when_not_changed() throws Exception {
        mockDetectorBehaviour(false, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        assertThat(extractByType(sub, GeofenceViolationsUpdateMessage.class), empty());
    }

    @Test
    public void send_alert_for_new_violation() throws Exception {
        mockDetectorBehaviour(true, false);

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        assertThat(extractByType(sub, AlertMessage.class), contains(
                new AlertMessage(new Alert(
                        "Geofence violation",
                        "Boundary violated by entity 'Goofy'",
                        SEVERE
                ))
        ));
    }

    @Test
    public void maintain_detector_state_between_calls() throws Exception {
        final State state = mock(State.class);
        when(detector.create(any())).thenReturn(state);

        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));

        verify(detector).next(eq(state), any());
    }

    @Test
    public void call_detector_with_layer_diff() throws Exception {
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer(), diff))));

        verify(detector).next(any(), eq(diff));
    }

    @Test
    public void ignore_non_live_layer_changes() throws Exception {
        when(layerSpec.getIndexable()).thenReturn(true);
        final List<Feature> diff = newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB));
        mockDetectorBehaviour(false, false);

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));
        snapshotSequences.onNext(sequence(just(snapshot(layer(), diff))));

        verify(detector, never()).next(any(), any());
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                featureCollection,
                null,
                0.0
        )));

        verify(converter, atLeastOnce()).toModel(featureCollection);
        verifyGeofences("custom", newArrayList(featureA, featureB));
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        snapshotSequences.onNext(sequence(just(snapshot(layer()))));
        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                layerId,
                0.0
        )));

        verifyGeofences("xyz", newArrayList(featureA, featureB));
    }

    @Test
    public void update_geofence_when_layer_updates() throws Exception {
        final NakedFeature featureC = nakedFeature(polygon(7.0));
        final NakedFeature featureD = nakedFeature(polygon(8.0));

        final PublishSubject<Snapshot> snapshots = PublishSubject.create();
        snapshotSequences.onNext(sequence(snapshots));

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                layerId,
                0.0
        )));

        snapshots.onNext(snapshot(layer()));
        snapshots.onNext(snapshot(layer(modelFeatureOf(featureC), modelFeatureOf(featureD))));

        verifyGeofences("xyz", newArrayList(featureA, featureB), newArrayList(featureC, featureD));
    }

    @Test
    public void set_empty_geofence_when_layer_empty() throws Exception {
        snapshotSequences.onNext(sequence(just(snapshot(layer(new Feature[] {})))));    // Empty!
        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                layerId,
                0.0
        )));

        verifyGeofences("", emptyList());
    }

    @Test
    public void set_empty_geofence_when_no_features_or_layer() throws Exception {
        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        )));

        verifyGeofences("", emptyList());
    }

    @Test
    public void set_empty_geofence_when_layer_missing() throws Exception {
        // Nothing added to snapshotSequences!
        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                layerId,
                0.0
        )));

        verifyGeofences("", emptyList());
    }

    @Test
    public void send_geometry_update_when_geofence_changes() throws Exception {
        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                featureCollection,
                null,
                0.0
        )));

        assertThat(extractByType(sub, GeofenceGeometryUpdateMessage.class),
                contains(new GeofenceGeometryUpdateMessage(featureCollection)));
    }

    @Test
    public void set_level_attribute_based_on_attribute_from_features() throws Exception {
        when(featureAttributes.attributes()).thenReturn(singletonMap(Geofence.Companion.getALERT_LEVEL(), "warning"));

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                mock(FeatureCollection.class),
                null,
                0.0
        )));

        verifyGeofences("custom", WARNING, newArrayList(featureA, featureB));
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                nakedFeature(point())
        ));

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                features,
                null,
                0.0
        )));

        verifyGeofences("custom", newArrayList(featureA));
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                nakedFeature(point())
        ));

        subscribeToHandler(status(new GeofenceStatus(
                INCLUDE,
                SEVERE,
                features,
                null,
                1.0
        )));

        verifyGeofences("custom", newArrayList(nakedFeature(bufferOp(point(), 1.0))));
    }

    @Test
    public void ignore_status_changes_not_involving_geofence_change() throws Exception {
        final GeofenceStatus gs = new GeofenceStatus(
                INCLUDE,
                SEVERE,
                null,
                null,
                0.0
        );
        final ClientStatusMessage statusA = status(gs);
        final ClientStatusMessage statusB = status(gs);
        when(statusA.getOpenLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));
        when(statusB.getOpenLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));

        subscribeToHandler(statusA, statusB);

        verify(detector, times(1)).create(any());
    }

    private ClientStatusMessage status(GeofenceStatus status) {
        ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.getGeofence()).thenReturn(status);
        return msg;
    }

    private void mockDetectorBehaviour(boolean newViolations, boolean hasChanged) {
        final Violation violation = mock(Violation.class);
        when(violation.getEntityId()).thenReturn(new EntityId("Goofy"));
        when(violation.getGeofenceId()).thenReturn(new EntityId("Pluto"));
        when(violation.getLevel()).thenReturn(SEVERE);

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
                features[0].stream().map(p -> geofenceOf(id, level, p.getGeometry())).collect(toList())
        );
    }

    private LayerSnapshotSequence sequence(Observable<Snapshot> snapshots) {
        return new LayerSnapshotSequence(layerSpec, snapshots);
    }

    private Snapshot snapshot(Layer layer) {
        return snapshot(layer, emptyList());
    }

    private Snapshot snapshot(Layer layer, List<Feature> diff) {
        return new Snapshot(mock(SnapshotId.class), layer, diff);
    }

    private Layer layer() {
        return layer(modelFeatureOf(featureA), modelFeatureOf(featureB));
    }

    private Layer layer(Feature... features) {
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);

        when(layer.getFeatures()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenAnswer(invocation -> stream(features));
        return layer;
    }

    private Geofence geofenceOf(String id, Alert.Level level, Geometry geometry) {
        return new Geofence(
                INCLUDE,
                new Feature(
                        new EntityId("geofence/" + id),
                        geometry,
                        AttributesImpl.of(singletonMap(Geofence.Companion.getALERT_LEVEL(), level))
                )
        );
    }

    private Feature modelFeatureOf(NakedFeature feature) {
        return new Feature(
                new EntityId("xyz"),
                feature.getGeometry(),
                feature.getAttributes());
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
        return new NakedFeature(null, polygon, featureAttributes);
    }

}
