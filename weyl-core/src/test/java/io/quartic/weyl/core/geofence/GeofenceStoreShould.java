package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.BiConsumer;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GeofenceStoreShould {
    private final UidGenerator<FeatureId> fidGen = new SequenceUidGenerator<>(FeatureId::of);
    private final GeofenceStore store = new GeofenceStore(mock(LayerStore.class), fidGen);
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

        verify(listener).onGeometryChange(ImmutableList.of(ImmutableFeature.of("99", FeatureId.of("1"), fenceGeometry, emptyMap())));
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
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true);
        updatePoint(false);

        verifyViolationDetails(GeofenceListener::onViolationBegin);
        verifyViolationDetails(GeofenceListener::onViolationEnd);
    }

    private void verifyViolationDetails(BiConsumer<GeofenceListener, Violation> consumer) {
        ArgumentCaptor<Violation> captor = ArgumentCaptor.forClass(Violation.class);
        consumer.accept(verify(listener), captor.capture());
        final Violation violation = captor.getValue();
        assertThat(violation.id(), equalTo(ViolationId.of("1")));
        assertThat(violation.geofenceId(), equalTo(GeofenceId.of("99")));
        assertThat(violation.featureExternalId(), equalTo("ducks"));
        assertThat(violation.message(), containsString("ducks"));
    }


    private void createGeofence(GeofenceType type) {
        store.setGeofences(ImmutableList.of(Geofence.of(GeofenceId.of("99"), type, fenceGeometry, ImmutableMap.of())));
    }

    private void updatePoint(boolean containsResult) {
        Geometry point = mock(Geometry.class);
        when(fenceGeometry.contains(point)).thenReturn(containsResult);
        store.onLiveLayerEvent(
                LayerId.of("666"),
                ImmutableFeature.builder()
                        .externalId("ducks")
                        .uid(FeatureId.of("123"))
                        .geometry(point)
                        .metadata(ImmutableMap.of())
                        .build());
    }
}
