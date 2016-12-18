package io.quartic.weyl.websocket;

import com.google.common.collect.Maps;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.AlertImpl;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.ViolationEvent;
import io.quartic.weyl.core.geofence.ViolationEvent.ViolationBeginEvent;
import io.quartic.weyl.core.geofence.ViolationEvent.ViolationClearEvent;
import io.quartic.weyl.core.geofence.ViolationEvent.ViolationEndEvent;
import io.quartic.weyl.core.geofence.ViolationEvent.Visitor;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.Subscription;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.common.rx.RxUtils.combine;
import static io.quartic.common.rx.RxUtils.latest;
import static io.quartic.common.rx.RxUtils.mealy;
import static io.quartic.weyl.core.model.Alert.Level.INFO;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Alert.Level.WARNING;
import static io.quartic.weyl.core.geofence.Geofence.ALERT_LEVEL;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static rx.Observable.empty;
import static rx.Observable.just;

public class GeofenceStatusHandler implements ClientStatusMessageHandler {
    private final GeofenceViolationDetector detector;
    private final Map<LayerId, Observable<Snapshot>> sequences = Maps.newConcurrentMap();
    private final Subscription subscription;
    private final FeatureConverter featureConverter;

    private static class State {
        private final Map<Alert.Level, Integer> counts = newHashMap();
        private final Set<Pair<EntityId, EntityId>> violations = newHashSet();
    }

    public GeofenceStatusHandler(GeofenceViolationDetector detector, Observable<LayerSnapshotSequence> snapshotSequences, FeatureConverter featureConverter) {
        this.detector = detector;
        this.featureConverter = featureConverter;
        this.subscription = snapshotSequences.subscribe(s -> sequences.put(s.spec().id(), s.snapshots()));
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return clientStatus
                .map(ClientStatusMessage::geofence)
                .distinctUntilChanged()
                .map(this::toGeofences)
                .compose(combine(this::generateGeometryUpdates, this::processViolations))
                .doOnUnsubscribe(subscription::unsubscribe);    // TODO: get rid of grossness
    }

    private Observable<SocketMessage> generateGeometryUpdates(Observable<Collection<Geofence>> geofenceStatuses) {
        return geofenceStatuses
                .map(geofences -> GeofenceGeometryUpdateMessageImpl.of(
                        featureConverter.toGeojson(geofences.stream().map(Geofence::feature).collect(toList()))
                ));
    }

    private Observable<SocketMessage> processViolations(Observable<Collection<Geofence>> geofenceStatuses) {
        return geofenceStatuses
                .compose(detector)
                .compose(mealy(new State(), this::nextState))
                .concatMap(x -> x);
    }

    private Collection<Geofence> toGeofences(GeofenceStatus status) {
        return toGeofences(
                status,
                status.enabled()
                        ? status.features().map(this::featuresFrom)
                        .orElse(status.layerId().map(this::featuresFrom).orElse(Stream.empty()))
                        : Stream.empty()
        );
    }

    private Collection<Geofence> toGeofences(GeofenceStatus status, Stream<Feature> features) {
        return features
                .map(f -> FeatureImpl.copyOf(f)
                        .withAttributes(AttributesImpl.of(singletonMap(ALERT_LEVEL, alertLevel(f, status.defaultLevel()))))
                        .withGeometry(bufferOp(f.geometry(), status.bufferDistance()))
                        .withEntityId(EntityIdImpl.of("geofence/" + f.entityId().uid()))
                )
                .filter(f -> !f.geometry().isEmpty())
                .map(f -> GeofenceImpl.of(status.type(), f))
                .collect(toList());
    }

    private Stream<Feature> featuresFrom(LayerId layerId) {
        // TODO: validate that layer exists
        return latest(sequences.getOrDefault(layerId, empty()))
                .absolute().features()
                .stream();
    }

    private Stream<Feature> featuresFrom(FeatureCollection features) {
        return featureConverter.toModel(features)
                .stream()
                .map(this::annotateFeature);
    }

    private Feature annotateFeature(NakedFeature feature) {
        return FeatureImpl.of(
                EntityIdImpl.of("custom"),
                feature.geometry(),
                feature.attributes()
        );
    }

    // TODO: this mutates the state :(
    private StateAndOutput<State, Observable<SocketMessage>> nextState(State state, ViolationEvent event) {
        return event.accept(new Visitor<StateAndOutput<State, Observable<SocketMessage>>>() {
            @Override
            public StateAndOutput<State, Observable<SocketMessage>> visit(ViolationBeginEvent event) {
                state.violations.add(Pair.of(event.entityId(), event.geofenceId()));
                state.counts.put(event.level(), state.counts.getOrDefault(event.level(), 0) + 1);
                return StateAndOutput.of(state, just(
                        violationUpdate(state),
                        alertMessageFor(event)
                ));
            }

            @Override
            public StateAndOutput<State, Observable<SocketMessage>> visit(ViolationEndEvent event) {
                state.violations.remove(Pair.of(event.entityId(), event.geofenceId()));
                state.counts.put(event.level(), state.counts.getOrDefault(event.level(), 1) - 1);   // Prevents going below 0 in cases that should never happen
                return StateAndOutput.of(state, just(violationUpdate(state)));
            }

            @Override
            public StateAndOutput<State, Observable<SocketMessage>> visit(ViolationClearEvent event) {
                final State nextState = new State();    // Reset
                return StateAndOutput.of(nextState, just(violationUpdate(nextState)));
            }

            private AlertMessageImpl alertMessageFor(ViolationBeginEvent event) {
                return AlertMessageImpl.of(AlertImpl.of(
                        "Geofence violation",
                        Optional.of("Boundary violated by entity '" + event.entityId() + "'"),
                        event.level()
                ));
            }
        });
    }

    private SocketMessage violationUpdate(State state) {
        return GeofenceViolationsUpdateMessageImpl.of(
                state.violations.stream().map(Pair::getRight).collect(toList()),
                state.counts.getOrDefault(INFO, 0),
                state.counts.getOrDefault(WARNING, 0),
                state.counts.getOrDefault(SEVERE, 0)
        );
    }
}
