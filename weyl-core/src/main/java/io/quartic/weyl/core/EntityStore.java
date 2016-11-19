package io.quartic.weyl.core;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class EntityStore {
    private final Map<EntityId, BehaviorSubject<AbstractFeature>> observables = newHashMap();

    public void putAll(Collection<AbstractFeature> features) {
        features.forEach(f -> getSubject(f.entityId()).onNext(f));
    }

    public Observable<AbstractFeature> get(EntityId id) {
        return getSubject(id);
    }

    private synchronized BehaviorSubject<AbstractFeature> getSubject(EntityId id) {
        BehaviorSubject<AbstractFeature> subject = observables.get(id);
        if (subject == null) {
            subject = BehaviorSubject.create();
            observables.put(id, subject);
        }
        return subject;
    }
}
