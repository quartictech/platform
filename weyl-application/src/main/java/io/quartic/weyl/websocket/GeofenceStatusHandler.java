package io.quartic.weyl.websocket;

import com.google.common.collect.Maps;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.Output;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AlertImpl;
import io.quartic.weyl.core.model.AttributesImpl;
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
import rx.Observable;
import rx.Subscription;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.common.rx.RxUtils.combine;
import static io.quartic.common.rx.RxUtils.latest;
import static io.quartic.common.rx.RxUtils.mealy;
import static io.quartic.weyl.core.geofence.Geofence.ALERT_LEVEL;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static io.quartic.weyl.core.model.Alert.Level.INFO;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Alert.Level.WARNING;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static rx.Observable.empty;
import static rx.Observable.just;

public class GeofenceStatusHandler implements ClientStatusMessageHandler {
    private final GeofenceViolationDetector detector;
    private final Observable<LayerSnapshotSequence> snapshotSequences;
    private final Map<LayerId, Observable<Snapshot>> sequences = Maps.newConcurrentMap();
    private final Subscription subscription;
    private final FeatureConverter featureConverter;

    public GeofenceStatusHandler(GeofenceViolationDetector detector, Observable<LayerSnapshotSequence> snapshotSequences, FeatureConverter featureConverter) {
        this.detector = detector;
        this.snapshotSequences = snapshotSequences;
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

    private Observable<SocketMessage> generateGeometryUpdates(Observable<Collection<Geofence>> geofenceStatuses) {
        return geofenceStatuses
                .map(geofences -> GeofenceGeometryUpdateMessageImpl.of(
                        featureConverter.toGeojson(geofences.stream().map(Geofence::feature).collect(toList()))
                ));
    }

    private Observable<SocketMessage> processViolations(Observable<Collection<Geofence>> geofenceStatuses) {
        // switchMap is fine - a few dupes/missing results due to resubscription are acceptable
        return geofenceStatuses.switchMap(this::detectViolationsForGeofenceStatus);
    }

    public Observable<SocketMessage> detectViolationsForGeofenceStatus(Collection<Geofence> geofences) {
        return diffs()
                .compose(mealy(detector.create(geofences), detector::next))
                .concatMap(this::processOutput);
    }

    private Observable<SocketMessage> processOutput(Output output) {
        return Observable.from(output.newViolations())
                .map(this::alertMessage)
                .concatWith(output.hasChanged() ? just(violationUpdateMessage(output)) : empty());
    }

    private Observable<Collection<Feature>> diffs() {
        return snapshotSequences
                .filter(seq -> !seq.spec().indexable())
                .flatMap(LayerSnapshotSequence::snapshots)
                .map(Snapshot::diff);
    }

    private SocketMessage violationUpdateMessage(Output output) {
        return GeofenceViolationsUpdateMessageImpl.of(
                output.violations().stream().map(Violation::geofenceId).collect(toList()),
                output.counts().getOrDefault(INFO, 0),
                output.counts().getOrDefault(WARNING, 0),
                output.counts().getOrDefault(SEVERE, 0)
        );
    }

    private SocketMessage alertMessage(Violation violation) {
        return AlertMessageImpl.of(AlertImpl.of(
                "Geofence violation",
                Optional.of("Boundary violated by entity '" + violation.entityId() + "'"),
                violation.level()
        ));
    }
}
