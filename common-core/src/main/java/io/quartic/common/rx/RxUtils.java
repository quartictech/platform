package io.quartic.common.rx;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public final class RxUtils {
    private RxUtils() {}

    @SweetStyle
    @Value.Immutable
    public interface WithPrevious<T> {
        @Nullable T prev();
        @Nullable T current();
    }

    public static <T> Transformer<T, WithPrevious<T>> pairWithPrevious(T initial) {
        return observable -> {
            final Observable<WithPrevious<T>> scan = observable
                    .scan(WithPreviousImpl.of(null, initial), (prev, current) -> WithPreviousImpl.of(prev.current(), current));
            return scan.skip(1);    // Because scan() emits the initial value
        };
    }

    /**
     * Make an observable act like a Behavior(Subject) - hot, and emits the current item on subscription.
     */
    public static <T> Transformer<T, T> likeBehavior() {
        return observable -> {
            ConnectableObservable<T> connectable = observable.replay(1);
            connectable.connect();
            connectable.subscribe();
            return connectable;
        };
    }

    /**
     * Note that this mutates the map, so only the most-recently emitted item is valid.
     */
    public static <R, K, V> Transformer<R, Map<K, V>> accumulateMap(Func1<R, K> toKey, Func1<R, V> toValue) {
        return observable -> observable.scan(newHashMap(), (prev, r) -> {
            prev.put(toKey.call(r), toValue.call(r));
            return prev;
        });
    }

    /**
     * Note that this mutates the set, so only the most-recently emitted item is valid.
     */
    public static <R, V> Transformer<R, Set<V>> accumulateSet(Func1<R, V> toValue) {
        return observable -> observable.scan(newHashSet(), (prev, r) -> {
            prev.add(toValue.call(r));
            return prev;
        });
    }

    /**
     * This only makes sense for observables that have behavior-like behaviour.
     */
    public static <T> T latest(Observable<T> observable) {
        return observable.first().toBlocking().single();
    }

    public static <T> List<T> all(Observable<T> observable) {
        return observable.toList().toBlocking().single();
    }
}