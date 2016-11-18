package io.quartic.weyl;

import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;

public class Multiplexer<T, U> {
    private final Function<T, Observable<U>> mapper;

    public static <T, U> Multiplexer<T, U> create(Function<T, Observable<U>> mapper) {
        return new Multiplexer<>(mapper);
    }

    public Multiplexer(Function<T, Observable<U>> mapper) {
        this.mapper = mapper;
    }

    public Observable<List<U>> multiplex(Observable<? extends Collection<T>> selection) {
        return selection.switchMap(sel -> combineLatest(collectUpstreams(sel), this::combine));
    }

    private List<Observable<U>> collectUpstreams(Collection<T> selection) {
        return selection.stream().map(mapper).collect(toList());
    }

    private List<U> combine(Object... objects) {
        return stream(objects).map(x -> (U)x).collect(toList());
    }
}
