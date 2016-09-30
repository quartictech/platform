package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Test;

import static io.quartic.weyl.core.utils.Utils.uuid;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeofenceStoreShould {
    private final GeofenceStore store = new GeofenceStore(mock(LiveLayerStore.class));
    private final Geometry fenceGeometry = mock(Geometry.class);

    @Test
    public void be_happy_if_inside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(true);

        assertThat(store.getGlobalState().ok(), is(true));
    }

    @Test
    public void be_sad_if_outside_inclusive_boundary() throws Exception {
        createGeofence(GeofenceType.INCLUDE);
        updatePoint(false);

        assertThat(store.getGlobalState().ok(), is(false));
    }

    @Test
    public void be_happy_if_outside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);

        assertThat(store.getGlobalState().ok(), is(true));
    }

    @Test
    public void be_sad_if_inside_exclusive_boundary() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(true);

        assertThat(store.getGlobalState().ok(), is(false));
    }

    @Test
    public void not_generate_violation_if_point_doesnt_violate() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(false);

        assertThat(store.getViolations(), empty());
    }

    @Test
    public void generate_violation_if_point_violates() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);

        assertThat(store.getViolations(), hasSize(1));
    }

    @Test
    public void generate_only_one_violation_if_point_stays_in_violation() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(true);

        assertThat(store.getViolations(), hasSize(1));
    }

    @Test
    public void generate_violation_each_time_point_violates() throws Exception {
        createGeofence(GeofenceType.EXCLUDE);
        updatePoint(false);
        updatePoint(true);
        updatePoint(false);
        updatePoint(true);

        assertThat(store.getViolations(), hasSize(2));
    }


    private void createGeofence(GeofenceType type) {
        store.setGeofence(Geofence.of(uuid(GeofenceId::of), type, fenceGeometry));
    }

    private void updatePoint(boolean containsResult) {
        Geometry point = mock(Geometry.class);
        when(fenceGeometry.contains(point)).thenReturn(containsResult);
        store.onLiveLayerEvent(LayerId.of("abc"), Feature.of(FeatureId.of("123"), point, ImmutableMap.of()));
    }
}
