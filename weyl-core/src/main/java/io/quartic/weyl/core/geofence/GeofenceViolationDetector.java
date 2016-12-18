package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.Transformer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.rx.RxUtils.mealy;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;

public class GeofenceViolationDetector implements Transformer<Collection<Geofence>, ViolationEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(GeofenceViolationDetector.class);
    private final Observable<LayerSnapshotSequence> snapshotSequences;

    @SweetStyle
    @Value.Immutable
    interface ViolationKey {
        EntityId entityId();
        EntityId geofenceId();
    }

    public GeofenceViolationDetector(Observable<LayerSnapshotSequence> snapshotSequences) {
        this.snapshotSequences = snapshotSequences;
    }

    @Override
    public Observable<ViolationEvent> call(Observable<Collection<Geofence>> geofenceStatuses) {
        // switchMap is fine - a few dupes/missing results due to resubscription are acceptable
        return geofenceStatuses.switchMap(this::detectViolationsForGeofenceStatus);
    }

    private Observable<ViolationEvent> detectViolationsForGeofenceStatus(Collection<Geofence> geofences) {
        final Set<ViolationKey> initial = newHashSet();
        return diffs()
                .compose(mealy(initial, (state, diffs) -> nextState(state, geofences, diffs)))
                .concatMap(Observable::from)
                .startWith(ViolationClearEventImpl.builder().build());
    }

    // Note that this actually just mutates the state, which is obviously cheating
    private StateAndOutput<Set<ViolationKey>, List<ViolationEvent>> nextState(
            Set<ViolationKey> state,
            Collection<Geofence> geofences,
            Collection<Feature> features
    ) {
        final List<ViolationEvent> output = newArrayList();

        features.forEach(feature -> geofences.forEach(geofence -> {
            final EntityId entityId = feature.entityId();
            final EntityId geofenceId = geofence.feature().entityId();
            final ViolationKeyImpl key = ViolationKeyImpl.of(entityId, geofenceId);

            final boolean violating = inViolation(geofence, feature);
            final boolean previouslyViolating = state.contains(key);

            if (violating && !previouslyViolating) {
                LOG.info("Violation begin: {} -> {}", entityId, geofenceId);
                output.add(ViolationBeginEventImpl.of(entityId, geofenceId, alertLevel(geofence.feature())));
                state.add(key);
            } else if (!violating && previouslyViolating) {
                LOG.info("Violation end: {} -> {}", entityId, geofenceId);
                output.add(ViolationEndEventImpl.of(entityId, geofenceId, alertLevel(geofence.feature())));
                state.remove(key);
            }
        }));

        return StateAndOutput.of(state, output);
    }

    private Observable<Collection<Feature>> diffs() {
        return snapshotSequences
                .filter(seq -> !seq.spec().indexable())
                .flatMap(LayerSnapshotSequence::snapshots)
                .map(Snapshot::diff);
    }

    private boolean inViolation(Geofence geofence, Feature feature) {
        final boolean contains = geofence.feature().geometry().contains(feature.geometry());
        return (geofence.type() == GeofenceType.INCLUDE && !contains) || (geofence.type() == GeofenceType.EXCLUDE && contains);
    }
}
