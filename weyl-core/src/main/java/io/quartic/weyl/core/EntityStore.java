package io.quartic.weyl.core;

import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class EntityStore {
    private final Map<EntityId, BehaviorSubject<Feature>> observables = newHashMap();

    public EntityStore(Observable<LayerSnapshotSequence> snapshotSequences) {
        snapshotSequences
                .flatMap(LayerSnapshotSequence::snapshots)
                .map(Snapshot::diff)
                .subscribe(values -> values.forEach(v -> getSubject(v.entityId()).onNext(v)));
    }

    public Observable<Feature> get(EntityId id) {
        return getSubject(id);
    }

    private synchronized BehaviorSubject<Feature> getSubject(EntityId id) {
        BehaviorSubject<Feature> subject = observables.get(id);
        if (subject == null) {
            subject = BehaviorSubject.create();
            observables.put(id, subject);
        }
        return subject;
    }
}
