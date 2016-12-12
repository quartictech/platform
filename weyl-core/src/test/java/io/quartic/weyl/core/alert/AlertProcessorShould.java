package io.quartic.weyl.core.alert;

import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AttributesImpl;
import org.hamcrest.Matchers;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AlertProcessorShould {

    private final AtomicReference<GeofenceListener> geofenceListener = new AtomicReference<>();
    private final GeofenceStore store = store(geofenceListener);
    private final AlertProcessor processor = new AlertProcessor(store);

    @Test
    public void generate_alert_from_violation() throws Exception {
        final TestSubscriber<Alert> sub = TestSubscriber.create();
        processor.alerts().subscribe(sub);

        geofenceListener.get().onViolationBegin(violation());

        sub.awaitValueCount(1, 250, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), Matchers.contains(AlertImpl.of(
                "Geofence violation",
                Optional.of("Absolute gimp"),
                Alert.Level.SEVERE
        )));
    }

    private GeofenceStore store(AtomicReference<GeofenceListener> listener) {
        final GeofenceStore store = mock(GeofenceStore.class);
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(store).addListener(any());
        return store;
    }

    private Violation violation() {
        final Violation violation = mock(Violation.class, RETURNS_DEEP_STUBS);
        when(violation.message()).thenReturn("Absolute gimp");
        when(violation.geofence().feature().attributes()).thenReturn(AttributesImpl.of(singletonMap(ALERT_LEVEL, Alert.Level.SEVERE)));
        return violation;
    }
}
