package io.quartic.common.rx;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;

public class PairWithPrevious<T> implements Observable.Transformer<T, PairWithPrevious.WithPrevious<T>> {
    @SweetStyle
    @Value.Immutable
    public interface WithPrevious<T> {
        @Nullable T prev();
        @Nullable T current();
    }

    private final T initial;

    public static <T> PairWithPrevious<T> pairWithPrevious(T initial) {
        return new PairWithPrevious<>(initial);
    }

    private PairWithPrevious(T initial) {
        this.initial = initial;
    }

    @Override
    public Observable<WithPrevious<T>> call(Observable<T> observable) {
        final Observable<WithPrevious<T>> scan = observable
                .scan(WithPreviousImpl.of(null, initial), (prev, current) -> WithPreviousImpl.of(prev.current(), current));
        return scan.skip(1);    // Because scan() emits the initial value
    }
}