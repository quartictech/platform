package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class GeofenceViolationDetectorShould {
    private final BehaviorSubject<LayerSnapshotSequence> snapshotSequences = BehaviorSubject.create();
    private final BehaviorSubject<Collection<Geofence>> geofenceStatuses = BehaviorSubject.create();
    private final GeofenceViolationDetector detector = new GeofenceViolationDetector(snapshotSequences);
    private final TestSubscriber<ViolationEvent> sub = TestSubscriber.create();
    private final Geometry fenceGeometry = mock(Geometry.class);

    @Before
    public void before() throws Exception {
        when(fenceGeometry.equals(fenceGeometry)).thenReturn(true);
        geofenceStatuses.compose(detector).subscribe(sub);
    }

    @Test
    public void not_notify_if_inside_inclusive_boundary() throws Exception {
        updateGeofence(GeofenceType.INCLUDE);
        updatePoint(true);

        assertSequence(clearEvent());
    }

    @Test
    public void notify_if_outside_inclusive_boundary() throws Exception {
        final Geofence geofence = updateGeofence(GeofenceType.INCLUDE);
        final Feature feature = updatePoint(false);

        assertSequence(clearEvent(), beginEvent(geofence, feature));
    }

    @Test
    public void not_notify_if_outside_exclusive_boundary() throws Exception {
        updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);

        assertSequence(clearEvent());
    }

    @Test
    public void notify_if_inside_exclusive_boundary() throws Exception {
        final Geofence geofence = updateGeofence(GeofenceType.EXCLUDE);
        final Feature feature = updatePoint(true);

        assertSequence(clearEvent(), beginEvent(geofence, feature));
    }

    @Test
    public void not_notify_if_point_continues_to_not_violate() throws Exception {
        updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(false);

        assertSequence(clearEvent());
    }

    @Test
    public void notify_if_point_switches_to_violating() throws Exception {
        final Geofence geofence = updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        final Feature feature = updatePoint(true);

        assertSequence(clearEvent(), beginEvent(geofence, feature));
    }

    @Test
    public void notify_only_once_if_point_continues_to_violate() throws Exception {
        final Geofence geofence = updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        final Feature feature = updatePoint(true);
        updatePoint(true);

        assertSequence(clearEvent(), beginEvent(geofence, feature));
    }

    @Test
    public void notify_each_time_point_switches_to_violating() throws Exception {
        final Geofence geofence = updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        final Feature featureA = updatePoint(true);
        final Feature featureB = updatePoint(false);
        final Feature featureC = updatePoint(true);

        assertSequence(
                clearEvent(),
                beginEvent(geofence, featureA),
                endEvent(geofence, featureB),
                beginEvent(geofence, featureC)
        );
    }

    @Test
    public void notify_when_geofences_reset() throws Exception {
        final Geofence geofenceA = updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        final Feature feature = updatePoint(true);
        final Geofence geofenceB = updateGeofence(GeofenceType.EXCLUDE);

        assertSequence(
                clearEvent(),
                beginEvent(geofenceA, feature),
                clearEvent(),
                beginEvent(geofenceB, feature)
        );
    }

    @Test
    public void ignore_changes_from_non_live_layers() throws Exception {
        updateGeofence(GeofenceType.EXCLUDE);
        updatePoint(true, point(), false);

        assertSequence(clearEvent());
    }

    private void assertSequence(ViolationEvent... events) {
        snapshotSequences.onCompleted();
        geofenceStatuses.onCompleted();
        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(events));
    }

    private Geofence updateGeofence(GeofenceType type) {
        final Geofence geofence = geofence(type);
        geofenceStatuses.onNext(ImmutableList.of(geofence));
        return geofence;
    }

    private Geofence geofence(GeofenceType type) {
        return GeofenceImpl.of(
                type,
                geofenceFeature()
        );
    }

    private Feature geofenceFeature() {
        return FeatureImpl.of(mock(EntityId.class), fenceGeometry, EMPTY_ATTRIBUTES);
    }

    private Feature updatePoint(boolean containsResult) {
        final Feature point = point();
        updatePoint(containsResult, point);
        return point;
    }

    private void updatePoint(boolean containsResult, Feature point) {
        updatePoint(containsResult, point, true);
    }

    private void updatePoint(boolean containsResult, Feature point, boolean live) {
        when(fenceGeometry.contains(point.geometry())).thenReturn(containsResult);

        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.diff()).thenReturn(newArrayList(point));

        final LayerSpec spec = mock(LayerSpec.class);
        when(spec.indexable()).thenReturn(!live);

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(spec, just(snapshot)));
    }

    private Feature point() {
        return FeatureImpl.builder()
                .entityId(EntityId.fromString("foo"))   // Use a fixed EntityId to represent evolution of a single entity
                .geometry(mock(Geometry.class))
                .attributes(EMPTY_ATTRIBUTES)
                .build();
    }

    private ViolationBeginEventImpl beginEvent(Geofence geofence, Feature feature) {
        return ViolationBeginEventImpl.of(feature.entityId(), geofence.feature().entityId(), alertLevel(geofence.feature()));
    }

    private ViolationEndEventImpl endEvent(Geofence geofence, Feature featureB) {
        return ViolationEndEventImpl.of(featureB.entityId(), geofence.feature().entityId(), alertLevel(geofence.feature()));
    }

    private ViolationEvent.ViolationClearEvent clearEvent() {
        return ViolationClearEventImpl.builder().build();
    }

}
