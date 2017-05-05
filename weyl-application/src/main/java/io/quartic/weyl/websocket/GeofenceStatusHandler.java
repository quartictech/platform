package io.quartic.weyl.websocket;

import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector.Output;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AlertImpl;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.websocket.message.AlertMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.slf4j.Logger;
import rx.Observable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.combine;
import static io.quartic.common.rx.RxUtilsKt.latest;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static io.quartic.common.rx.RxUtilsKt.mealy;
import static io.quartic.weyl.core.feature.FeatureConverter.MINIMAL_MANIPULATOR;
import static io.quartic.weyl.core.geofence.Geofence.ALERT_LEVEL;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static io.quartic.weyl.core.model.Alert.Level.INFO;
import static io.quartic.weyl.core.model.Alert.Level.SEVERE;
import static io.quartic.weyl.core.model.Alert.Level.WARNING;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Observable.empty;
import static rx.Observable.from;
import static rx.Observable.just;

public class GeofenceStatusHandler implements ClientStatusMessageHandler {
    private static final Logger LOG = getLogger(GeofenceStatusHandler.class);
    private final GeofenceViolationDetector detector;
    private final Observable<LayerSnapshotSequence> snapshotSequences;
    private final Observable<Map<LayerId, Observable<Snapshot>>> sequenceMap;
    private final FeatureConverter featureConverter;

    public GeofenceStatusHandler(Observable<LayerSnapshotSequence> snapshotSequences, GeofenceViolationDetector detector, FeatureConverter featureConverter) {
        this.detector = detector;
        this.snapshotSequences = snapshotSequences;
        this.featureConverter = featureConverter;
        // Each item is a map from all current layer IDs to the corresponding snapshot sequence for that layer
        this.sequenceMap = snapshotSequences
                .compose(accumulateMap(seq -> seq.spec().id(), LayerSnapshotSequence::snapshots))
                .compose(likeBehavior());
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return clientStatus
                .map(ClientStatusMessage::getGeofence)
                .distinctUntilChanged()
                .switchMap(this::toGeofences)   // TODO: is switchMap a good idea here?  Means we resubscribe to sequenceMap all the time
                .compose(combine(this::generateGeometryUpdates, this::processViolations));
    }

    private Observable<Collection<Geofence>> toGeofences(GeofenceStatus status) {
        // TODO: eliminate ofNullable once we convert to Kotlin
        return ofNullable(status.getLayerId()).map(this::featuresFrom)
                .orElse(ofNullable(status.getFeatures()).map(this::featuresFrom).orElse(just(emptyList())))
                .map(f -> toGeofences(status, f));
    }

    private Collection<Geofence> toGeofences(GeofenceStatus status, Collection<Feature> features) {
        return features.stream()
                .map(f -> toGeofence(status, f))
                .filter(g -> !g.feature().geometry().isEmpty())
                .collect(toList());
    }

    private Geofence toGeofence(GeofenceStatus status, Feature f) {
        return GeofenceImpl.of(
                status.getType(),
                FeatureImpl.of(
                        new EntityId("geofence/" + f.entityId().getUid()),
                        bufferOp(f.geometry(), status.getBufferDistance()),
                        AttributesImpl.of(singletonMap(ALERT_LEVEL, alertLevel(f, status.getDefaultLevel())))
                )
        );
    }

    private Observable<Collection<Feature>> featuresFrom(LayerId layerId) {
        // Handling of deleted layers is implicit - we rely on the final snapshot being empty -> no geofences
        return ofNullable(latest(sequenceMap).get(layerId))
                .map(snapshots -> snapshots.map(s -> (Collection<Feature>) s.absolute().features()))
                .orElseGet(() -> {
                    LOG.warn("Trying to create geofence from non-existent layer with id {}", layerId);
                    return just(emptyList());   // No geofences in the case of missing layer
                });
    }

    private Observable<Collection<Feature>> featuresFrom(FeatureCollection features) {
        return just(featureConverter.toModel(features)
                .stream()
                .map(this::annotateFeature)
                .collect(toList()));
    }

    private Feature annotateFeature(NakedFeature feature) {
        return FeatureImpl.of(
                new EntityId("custom"),
                feature.geometry(),
                feature.attributes()
        );
    }

    private Observable<SocketMessage> generateGeometryUpdates(Observable<Collection<Geofence>> geofenceStatuses) {
        return geofenceStatuses
                .map(geofences -> new GeofenceGeometryUpdateMessage(
                        featureConverter.toGeojson(MINIMAL_MANIPULATOR, geofences.stream().map(Geofence::feature).collect(toList()))
                ));
    }

    private Observable<SocketMessage> processViolations(Observable<Collection<Geofence>> geofenceStatuses) {
        // switchMap is fine - a few dupes/missing results due to resubscription are acceptable
        return geofenceStatuses.switchMap(this::detectViolationsForGeofenceStatus);
    }

    private Observable<SocketMessage> detectViolationsForGeofenceStatus(Collection<Geofence> geofences) {
        return snapshotSequences
                .compose(this::extractLiveFeatures)
                .compose(mealy(detector.create(geofences), detector::next))
                .concatMap(this::processOutput);
    }

    private Observable<Collection<Feature>> extractLiveFeatures(Observable<LayerSnapshotSequence> sequences) {
        return sequences
                .filter(seq -> !seq.spec().indexable())
                .flatMap(LayerSnapshotSequence::snapshots)
                .map(snapshot -> snapshot.diff().features());
    }

    private Observable<SocketMessage> processOutput(Output output) {
        return from(output.newViolations())
                .map(this::alertMessage)
                .concatWith(output.hasChanged() ? just(violationUpdateMessage(output)) : empty());
    }

    private SocketMessage violationUpdateMessage(Output output) {
        return new GeofenceViolationsUpdateMessage(
                output.violations().stream().map(Violation::geofenceId).collect(toSet()),
                output.counts().get(INFO),
                output.counts().get(WARNING),
                output.counts().get(SEVERE)
        );
    }

    private SocketMessage alertMessage(Violation violation) {
        return new AlertMessage(AlertImpl.of(
                "Geofence violation",
                Optional.of("Boundary violated by entity '" + violation.entityId() + "'"),
                violation.level()
        ));
    }
}
