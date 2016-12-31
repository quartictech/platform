package io.quartic.common.rx;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.util.ObserverSubscriber;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static rx.Observable.merge;

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

    @SweetStyle
    @Value.Immutable
    public interface StateAndOutput<State, Output> {
        State state();
        @Nullable Output output();

        static <State, Output> StateAndOutput<State, Output> of(State state, Output output) {
            return StateAndOutputImpl.of(state, output);
        }
    }

    /**
     * Implements a Mealy state machine, supporting computation of the form
     * {@code {state[n], output[n]} = f(state[n-1], input[n])}.
     *
     * <p>
     * This is for cases where {@link rx.Observable#scan(Object, Func2)} would be ugly because of the need to
     * shoehorn state and output into the same type.
     */
    public static <Input, State, Output> Transformer<Input, Output> mealy(State initial, Func2<State, Input, StateAndOutput<State, Output>> next) {
        final StateAndOutput<State, Output> wrappedInitial = StateAndOutput.of(initial, null);
        final Func2<StateAndOutput<State, Output>, Input, StateAndOutput<State, Output>> wrappedNext =
                (wrapped, input) -> next.call(wrapped.state(), input);
        return observable -> observable
                .scan(wrappedInitial, wrappedNext)
                .skip(1)                        // Because scan() emits the initial value
                .map(StateAndOutput::output);
    }

    /**
     * Make an observable act like a Behavior(Subject) - hot, and emits the current item on subscription.
     */
    public static <T> Transformer<T, T> likeBehavior() {
        return observable -> {
            BehaviorSubject<T> bs = BehaviorSubject.create();
            observable.subscribe(new ObserverSubscriber<>(bs));
            return bs;
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

    @SafeVarargs
    public static <T, R> Transformer<T, R> combine(Transformer<T, ? extends R>... transformers) {
        return combine(asList(transformers));
    }

    public static <T, R> Transformer<T, R> combine(Collection<? extends Transformer<T, ? extends R>> transformers) {
        return observable -> {
            final Observable<T> shared = observable.share();
            return merge(transformers.stream().map(shared::compose).collect(toList()));
        };
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