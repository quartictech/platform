package io.quartic.weyl.core.alert;

public class AlertProcessorShould {

//    private final AtomicReference<GeofenceListener> geofenceListener = new AtomicReference<>();
//    private final GeofenceViolationDetector store = store(geofenceListener);
//    private final AlertProcessor processor = new AlertProcessor(store);
//
//    @Test
//    public void generate_alert_from_violation() throws Exception {
//        final TestSubscriber<Alert> sub = TestSubscriber.create();
//        processor.alerts().subscribe(sub);
//
//        geofenceListener.get().onViolationBegin(violation());
//
//        sub.awaitValueCount(1, 250, MILLISECONDS);
//        assertThat(sub.getOnNextEvents(), Matchers.contains(AlertImpl.of(
//                "Geofence violation",
//                Optional.of("Boundary violated by entity 'Goofy'"),
//                Alert.Level.SEVERE
//        )));
//    }
//
//    private GeofenceViolationDetector store(AtomicReference<GeofenceListener> listener) {
//        final GeofenceViolationDetector store = mock(GeofenceViolationDetector.class);
////        doAnswer(invocation -> {
////            listener.set(invocation.getArgument(0));
////            return null;
////        }).when(store).addListener(any());
//        return store;
//    }
//
//    private Violation violation() {
//        final Violation violation = mock(Violation.class, RETURNS_DEEP_STUBS);
//        when(violation.feature().entityId()).thenReturn(EntityId.fromString("Goofy"));
//        when(violation.geofence().feature().attributes()).thenReturn(AttributesImpl.of(singletonMap(ALERT_LEVEL, Alert.Level.SEVERE)));
//        return violation;
//    }
}
