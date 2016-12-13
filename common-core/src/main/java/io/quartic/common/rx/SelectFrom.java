package io.quartic.common.rx;

import rx.Observable;
import rx.functions.Func1;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static rx.Observable.combineLatest;
import static rx.Observable.empty;
import static rx.Observable.switchOnNext;

/**
 * Treats the input observable as a sequence of keys into a map of observables, switching between observables as the
 * key changes.  The map is accumulated from another "raw" observable (thus there's no way to ever delete an entry from
 * the map).
 */
public class SelectFrom<R, K, V> implements Observable.Transformer<K, V> {
    private final Observable<Map<K, Observable<V>>> map;

    public static <R, K, V> SelectFrom<R, K, V> getLatestFrom(Observable<R> raw, Func1<R, K> toKey, Func1<R, Observable<V>> toValues) {
        return new SelectFrom<>(raw, toKey, toValues);
    }

    private SelectFrom(Observable<R> raw, Func1<R, K> toKey, Func1<R, Observable<V>> toValues) {
        this.map = raw.scan(
                newHashMap(),
                (map, r) -> addToMap(map, toKey.call(r), toValues.call(r))
        );
    }

    private static <K, V> Map<K, V> addToMap(Map<K, V> map, K key, V value) {
        map.put(key, value);
        return map;
    }

    @Override
    public Observable<V> call(Observable<K> keys) {
        return switchOnNext(
                combineLatest(keys, map, this::getOrEmpty)
                        .distinctUntilChanged() // Avoid resubscribing to the same value observable when an entry gets added to the map
        );
    }

    private Observable<V> getOrEmpty(K key, Map<K, Observable<V>> values) {
        return values.getOrDefault(key, empty());
    }
}
