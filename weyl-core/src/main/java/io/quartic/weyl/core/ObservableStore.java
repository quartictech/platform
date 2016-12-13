package io.quartic.weyl.core;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.Maps.newHashMap;

public class ObservableStore<K, V> {
    private final Map<K, BehaviorSubject<V>> observables = newHashMap();
    private final boolean emptyOnMissingKey;

    public ObservableStore() {
        this.emptyOnMissingKey = false;
    }

    public ObservableStore(boolean emptyOnMissingKey) {
        this.emptyOnMissingKey = emptyOnMissingKey;
    }

    public Observable<V> get(K id) {
        return getSubject(id, emptyOnMissingKey);
    }

    public void put(K id, V value) {
        getSubject(id, false).onNext(value);
    }

    public void putAll(Function<V, K> id, Collection<V> values) {
        values.forEach(v -> put(id.apply(v), v));
    }

    private synchronized BehaviorSubject<V> getSubject(K id, boolean emptyOnMissingKey) {
        BehaviorSubject<V> subject = observables.get(id);
        if (subject == null) {
            subject = BehaviorSubject.create();

            if (emptyOnMissingKey) {
                subject.onCompleted();
            } else {
                observables.put(id, subject);
            }
        }
        return subject;
    }
}
