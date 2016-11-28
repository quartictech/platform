package io.quartic.weyl.core;

import io.quartic.weyl.core.model.Feature;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.Maps.newHashMap;

public class ObservableStore<K, V> {
    private final Map<K, BehaviorSubject<V>> observables = newHashMap();

    public void putAll(Collection<V> values, Function<V, K> id) {
        values.forEach(v -> getSubject(id.apply(v)).onNext(v));
    }

    public Observable<V> get(K id) {
        return getSubject(id);
    }

    public void put(K id, V value) {
        getSubject(id).onNext(value);
    }

    public void remove(K id) {
        getSubject(id).onCompleted();
        observables.remove(id);
    }

    private synchronized BehaviorSubject<V> getSubject(K id) {
        BehaviorSubject<V> subject = observables.get(id);
        if (subject == null) {
            subject = BehaviorSubject.create();
            observables.put(id, subject);
        }
        return subject;
    }
}
