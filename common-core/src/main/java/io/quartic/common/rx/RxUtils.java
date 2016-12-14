package io.quartic.common.rx;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;
import rx.observables.ConnectableObservable;

public final class RxUtils {
    private RxUtils() {}

    @SweetStyle
    @Value.Immutable
    public interface WithPrevious<T> {
        @Nullable T prev();
        @Nullable T current();
    }

    public static <T> Observable.Transformer<T, WithPrevious<T>> pairWithPrevious(T initial) {
        return observable -> {
            final Observable<WithPrevious<T>> scan = observable
                    .scan(WithPreviousImpl.of(null, initial), (prev, current) -> WithPreviousImpl.of(prev.current(), current));
            return scan.skip(1);    // Because scan() emits the initial value
        };
    }

    /**
     * Make an observable act like a Behavior(Subject) - hot, and emits the current item on subscription.
     */
    public static <T> Observable.Transformer<T, T> likeBehavior() {
        return observable -> {
            ConnectableObservable<T> connectable = observable.replay(1);
            connectable.connect();
            connectable.subscribe();
            return connectable;
        };
    }
}