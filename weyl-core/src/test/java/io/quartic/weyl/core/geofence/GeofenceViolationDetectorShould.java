package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.rx.StateAndOutput;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.Output;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.State;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static io.quartic.weyl.core.geofence.GeofenceType.EXCLUDE;
import static io.quartic.weyl.core.geofence.GeofenceType.INCLUDE;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeofenceViolationDetectorShould {
    private final GeofenceViolationDetector detector = new GeofenceViolationDetector();
    private final Geometry fenceGeometry = mock(Geometry.class);
    private State state;

    @Test
    public void not_detect_if_inside_inclusive_boundary() throws Exception {
        resetDetector(geofence(INCLUDE));
        final Output output = runDetector(point(true));

        assertThat(output.newViolations(), empty());
    }

    @Test
    public void detect_if_outside_inclusive_boundary() throws Exception {
        final Geofence geofence = geofence(INCLUDE);
        final Feature feature = point(false);

        resetDetector(geofence);
        final Output output = runDetector(feature);

        assertThat(output.newViolations(), contains(violation(geofence, feature)));
    }

    @Test
    public void not_detect_if_outside_exclusive_boundary() throws Exception {
        resetDetector(geofence(EXCLUDE));
        final Output output = runDetector(point(false));

        assertThat(output.newViolations(), empty());
    }

    @Test
    public void detect_if_inside_exclusive_boundary() throws Exception {
        final Geofence geofence = geofence(EXCLUDE);
        final Feature feature = point(true);

        resetDetector(geofence);
        final Output output = runDetector(feature);

        assertThat(output.newViolations(), contains(violation(geofence, feature)));
    }

    @Test
    public void detect_only_once_if_point_continues_to_violate() throws Exception {
        final Feature feature = point(true);

        resetDetector(geofence(EXCLUDE));
        runDetector(feature);
        final Output output = runDetector(feature);

        assertThat(output.newViolations(), empty());
    }

    @Test
    public void not_detect_change_if_no_change() throws Exception {
        final Feature feature = point(true);

        resetDetector(geofence(EXCLUDE));
        runDetector(feature);
        final Output output = runDetector(feature);

        assertThat(output.hasChanged(), equalTo(false));
    }

    @Test
    public void detect_change_if_positive_change() throws Exception {
        resetDetector(geofence(EXCLUDE));
        runDetector(point(false));
        final Output output = runDetector(point(true));

        assertThat(output.hasChanged(), equalTo(true));
    }

    @Test
    public void detect_change_if_negative_change() throws Exception {
        resetDetector(geofence(EXCLUDE));
        runDetector(point(true));
        final Output output = runDetector(point(false));

        assertThat(output.hasChanged(), equalTo(true));
    }

    @Test
    public void always_detect_change_on_first_run() throws Exception {
        resetDetector(geofence(EXCLUDE));
        final Output output = runDetector(point(false)); // No new violations

        assertThat(output.hasChanged(), equalTo(true));
    }

    @Test
    public void accumulate_violations_and_counts() throws Exception {
        final Geofence geofence = geofence(EXCLUDE);
        final Feature feature = point(true);

        resetDetector(geofence);
        final Output outputA = runDetector(feature);

        assertThat(outputA.violations(), contains(violation(geofence, feature)));
        assertThat(outputA.counts().get(SEVERE), equalTo(1));

        final Output outputB = runDetector(point(false));

        assertThat(outputB.violations(), empty());
        assertThat(outputA.counts().get(SEVERE), equalTo(0));
    }

    private void resetDetector(Geofence geofence) {
        state = detector.create(newArrayList(geofence));
    }

    private Output runDetector(Feature feature) {
        final StateAndOutput<State, Output> next = detector.next(state, newArrayList(feature));
        state = next.getState();
        return next.getOutput();
    }

    private Geofence geofence(GeofenceType type) {
        return GeofenceImpl.of(type, geofenceFeature());
    }

    private Feature geofenceFeature() {
        return FeatureImpl.of(mock(EntityId.class), fenceGeometry, EMPTY_ATTRIBUTES);
    }

    private Feature point(boolean containsResult) {
        final Feature point = FeatureImpl.builder()
                .entityId(EntityId.fromString("foo"))   // Use a fixed EntityId to represent evolution of a single entity
                .geometry(mock(Geometry.class))
                .attributes(EMPTY_ATTRIBUTES)
                .build();
        when(fenceGeometry.contains(point.geometry())).thenReturn(containsResult);
        return point;
    }

    private Violation violation(Geofence geofence, Feature feature) {
        return ViolationImpl.of(feature.entityId(), geofence.feature().entityId(), alertLevel(geofence.feature()));
    }
}
