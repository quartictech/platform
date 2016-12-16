package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.subjects.PublishSubject;

import java.util.function.BiConsumer;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class GeofenceStoreShould {
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final GeofenceStore store = new GeofenceStore(snapshotSequences);
    private final GeofenceListener listener = mock(GeofenceListener.class);
    private final Geometry fenceGeometry = mock(Geometry.class);

    @Before
    public void setUp() throws Exception {
        store.addListener(listener);
        when(fenceGeometry.equals(fenceGeometry)).thenReturn(true);
    }

    @Test
    public void notify_on_geometry_change() throws Exception {
        createGeofence(GeofenceType.INCLUDE);

        verify(listener).onGeometryChange(ImmutableList.of(
                FeatureImpl.of(EntityIdImpl.of("geofence/99"), fenceGeometry, EMPTY_ATTRIBUTES)
        ));
    }

    @Test
    public void not_notify_if_inside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(true);

        verify(listener, never()).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void notify_if_outside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(false);

        verify(listener).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void not_notify_if_outside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);

        verify(listener, never()).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void notify_if_inside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true);

        verify(listener).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void not_notify_if_point_continues_to_not_violate() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(false);

        verify(listener, never()).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void notify_if_point_switches_to_violating() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);

        verify(listener).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void notify_only_once_if_point_continues_to_violate() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(true);

        verify(listener).onViolationBegin(any());
        verify(listener, never()).onViolationEnd(any());
    }

    @Test
    public void notify_each_time_point_switches_to_violating() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(false);
        updatePoint(true);

        verify(listener, times(2)).onViolationBegin(any());
        verify(listener, times(1)).onViolationEnd(any());
    }

    @Test
    public void notify_when_geofences_reset() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        createGeofence(GeofenceType.EXCLUDE);

        verify(listener, times(1)).onViolationEnd(any());
    }

    @Test
    public void include_relevant_details_in_violation() throws Exception {
        final Feature point = point();
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true, point);
        updatePoint(false, point);

        verifyViolationDetails(GeofenceListener::onViolationBegin, point);
        verifyViolationDetails(GeofenceListener::onViolationEnd, point);
    }

    @Test
    public void ignore_changes_from_non_live_layers() throws Exception {
        final Feature point = point();
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true, point, false);

        verify(listener, never()).onViolationBegin(any());
    }

    private void verifyViolationDetails(BiConsumer<GeofenceListener, Violation> consumer, Feature point) {
        ArgumentCaptor<Violation> captor = ArgumentCaptor.forClass(Violation.class);
        consumer.accept(verify(listener), captor.capture());
        final Violation violation = captor.getValue();
        assertThat(violation.geofence(), equalTo(geofence(GeofenceType.EXCLUDE)));
        assertThat(violation.feature(), equalTo(point));
        assertThat(violation.message(), containsString("ducks"));
    }

    private void createGeofence(GeofenceType type) {
        store.setGeofences(ImmutableList.of(geofence(type)));
    }

    private Geofence geofence(GeofenceType type) {
        return GeofenceImpl.of(
                type,
                geofenceFeature()
        );
    }

    private Feature geofenceFeature() {
        return FeatureImpl.of(EntityIdImpl.of("geofence/99"), fenceGeometry, EMPTY_ATTRIBUTES);
    }

    private void updatePoint(boolean containsResult) {
        final Feature point = point();
        updatePoint(containsResult, point);
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
                .entityId(EntityIdImpl.of("666/ducks"))
                .geometry(mock(Geometry.class))
                .attributes(EMPTY_ATTRIBUTES)
                .build();
    }

}
