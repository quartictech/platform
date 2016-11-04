package io.quartic.terminator;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Map;

import static com.google.common.collect.Sets.difference;
import static java.util.Collections.emptyMap;
import static rx.Observable.concat;
import static rx.Observable.from;

public class ChangeCalculator<K, V, R> implements Func1<Map<? extends K, V>, Observable<R>> {
    private final Func2<? super K, V, R> onAdded;
    private final Func2<? super K, V, R> onRemoved;
    private Map<? extends K, V> current;

    public static <K, V, R> ChangeCalculator<K, V, R> create(Func2<? super K, V, R> onAdded, Func2<? super K, V, R> onRemoved) {
        return new ChangeCalculator<>(onAdded, onRemoved, emptyMap());
    }

    public static <K, V, R> ChangeCalculator<K, V, R> create(Func2<? super K, V, R> onAdded, Func2<? super K, V, R> onRemoved, Map<K, V> initialState) {
        return new ChangeCalculator<>(onAdded, onRemoved, initialState);
    }

    private ChangeCalculator(Func2<? super K, V, R> onAdded, Func2<? super K, V, R> onRemoved, Map<? extends K, V> initialState) {
        this.onAdded = onAdded;
        this.onRemoved = onRemoved;
        this.current = initialState;
    }

    @Override
    public Observable<R> call(Map<? extends K, V> next) {
        final Map<? extends K, V> previous = current;
        current = next;

        // TODO: handle config changes
        return concat(
                from(difference(current.keySet(), previous.keySet())).map(k -> onAdded.call(k, current.get(k))),
                from(difference(previous.keySet(), current.keySet())).map(k -> onRemoved.call(k, previous.get(k)))
        );
    }
}
