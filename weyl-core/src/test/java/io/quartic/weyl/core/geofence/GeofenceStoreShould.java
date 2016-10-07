package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.UidGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class GeofenceStoreShould {
    private final GeofenceStore store = new GeofenceStore(mock(LiveLayerStore.class));
    private final ViolationListener listener = mock(ViolationListener.class);
    private final Geometry fenceGeometry = mock(Geometry.class);
    private final UidGenerator<GeofenceId> gidGen = new UidGenerator<>(GeofenceId::of);

    @Before
    public void setUp() throws Exception {
        store.addListener(listener);
    }

    @Test
    public void not_notify_if_inside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(true);

        verifyZeroInteractions(listener);
    }

    @Test
    public void notify_if_outside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(false);

        verify(listener).onViolation(any());
    }

    @Test
    public void not_notify_if_outside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);

        verifyZeroInteractions(listener);
    }

    @Test
    public void notify_if_inside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true);

        verify(listener).onViolation(any());
    }

    @Test
    public void not_notify_if_point_continues_to_not_violate() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(false);

        verifyZeroInteractions(listener);
    }

    @Test
    public void notify_if_point_switches_to_violating() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);

        verify(listener).onViolation(any());
    }

    @Test
    public void notify_only_once_if_point_continues_to_violate() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(true);

        verify(listener).onViolation(any());
    }

    @Test
    public void notify_each_time_point_switches_to_violating() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(false);
        updatePoint(true);

        verify(listener, times(2)).onViolation(any());
    }


    private void createGeofence(GeofenceType type) {
        store.setGeofence(Geofence.of(gidGen.get(), type, fenceGeometry));
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
