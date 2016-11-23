package io.quartic.weyl;

import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;
import static rx.Observable.just;

public class Multiplexer<T, K, V> implements Observable.Transformer<Pair<T, List<K>>, Pair<T, List<V>>> {
    private final Function<K, Observable<V>> mapper;

    public static <T, K, V> Multiplexer<T, K, V> create(Function<K, Observable<V>> mapper) {
        return new Multiplexer<>(mapper);
    }

    private Multiplexer(Function<K, Observable<V>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Observable<Pair<T, List<V>>> call(Observable<Pair<T, List<K>>> selection) {
        return selection.switchMap(sel -> sel.getRight().isEmpty()
                ? just(Pair.of(sel.getLeft(), emptyList()))         // Emit an update even in the case of empty selection
                : combineLatest(collectUpstreams(sel.getRight()), o -> Pair.of(sel.getLeft(), combine(o))));
    }

    private List<Observable<V>> collectUpstreams(Collection<K> selection) {
        return selection.stream().map(mapper).collect(toList());
    }

    private List<V> combine(Object... objects) {
        return stream(objects).map(x -> (V)x).collect(toList());
    }
}
