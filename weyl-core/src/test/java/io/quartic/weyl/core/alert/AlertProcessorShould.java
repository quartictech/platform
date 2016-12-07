package io.quartic.weyl.core.alert;

import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Attributes;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.weyl.core.alert.AlertProcessor.GEOFENCE_LEVEL;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AlertProcessorShould {

    private final AtomicReference<GeofenceListener> geofenceListener = new AtomicReference<>();
    private final GeofenceStore store = store(geofenceListener);
    private final AlertProcessor processor = new AlertProcessor(store);

    @Test
    public void generate_alert_from_violation_without_level_attribute() throws Exception {
        final AlertListener listener = mock(AlertListener.class);
        processor.addListener(listener);

        geofenceListener.get().onViolationBegin(violation(EMPTY_ATTRIBUTES));

        verifyAlertGenerated(listener, Alert.Level.SEVERE);
    }

    @Test
    public void generate_alert_from_violation_with_bad_level_attribute() throws Exception {
        final AlertListener listener = mock(AlertListener.class);
        processor.addListener(listener);

        final Violation violation = violation(() -> map(entry(GEOFENCE_LEVEL, "gimpery")));
        geofenceListener.get().onViolationBegin(violation);

        verifyAlertGenerated(listener, Alert.Level.SEVERE);
    }

    @Test
    public void generate_alert_from_violation_with_level_attribute() throws Exception {
        final AlertListener listener = mock(AlertListener.class);
        processor.addListener(listener);

        final Violation violation = violation(() -> map(entry(GEOFENCE_LEVEL, "info")));
        geofenceListener.get().onViolationBegin(violation);

        verifyAlertGenerated(listener, Alert.Level.INFO);
    }


    private void verifyAlertGenerated(AlertListener listener, Alert.Level level) {
        verify(listener).onAlert(AlertImpl.of(
                "Geofence violation",
                Optional.of("Absolute gimp"),
                level
        ));
    }

    private GeofenceStore store(AtomicReference<GeofenceListener> listener) {
        final GeofenceStore store = mock(GeofenceStore.class);
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(store).addListener(any());
        return store;
    }

    private Violation violation(Attributes geofenceAttributes) {
        final Violation violation = mock(Violation.class, RETURNS_DEEP_STUBS);
        when(violation.message()).thenReturn("Absolute gimp");
        when(violation.geofence().feature().attributes()).thenReturn(geofenceAttributes);
        return violation;
    }
}
