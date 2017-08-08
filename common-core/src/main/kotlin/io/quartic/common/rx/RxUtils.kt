package io.quartic.common.rx

import rx.Observable
import rx.Observable.Transformer
import rx.Observable.merge
import rx.internal.util.ObserverSubscriber
import rx.subjects.BehaviorSubject
import java.util.Arrays.asList

fun <T> pairWithPrevious(initial: T) = Transformer<T, WithPrevious<T>> { observable ->
    observable
            .scan(WithPrevious(null, initial), { prev, current -> WithPrevious(prev.current, current) })
            .skip(1)    // Because scan() emits the initial value
}

data class WithPrevious<out T>(
        val prev: T?,
        val current: T?
)

/**
 * Implements a Mealy state machine, supporting computation of the form
 * `{state[n], output[n]} = f(state[n-1], input[n])`.
 *
 * This is for cases where [rx.Observable.scan] would be ugly because of the need to
 * shoehorn state and output into the same type.
 */
fun <Input, State, Output>
        mealy(initial: State, next: (State, Input) -> StateAndOutput<State, Output>) = Transformer<Input, Output> {
    observable -> observable
        .scan(StateAndOutput(initial, null as Output?), { wrapped, input -> next(wrapped.state, input) })
        .skip(1)                        // Because scan() emits the initial value
        .map({ it.output })
}

data class StateAndOutput<out State, out Output>(
        val state: State,
        val output: Output?
)

/**
 * Make an observable act like a Behavior(Subject) - hot, and emits the current item on subscription.
 */
fun <T> likeBehavior() = Transformer<T, T> { observable ->
    val bs = BehaviorSubject.create<T>()
    observable.subscribe(ObserverSubscriber(bs))
    bs
}

// TODO: make immutable
/**
 * Note that this mutates the map, so only the most-recently emitted item is valid.
 */
fun <R, K, V> accumulateMap(toKey: (R) -> K, toValue: (R) -> V) = Transformer<R, MutableMap<K, V>> { observable ->
    observable.scan(mutableMapOf(), { prev, r ->
        prev.put(toKey(r), toValue(r))
        prev
    })
}

// TODO: make immutable
/**
 * Note that this mutates the set, so only the most-recently emitted item is valid.
 */
fun <R, V> accumulateSet(toValue: (R) -> V) = Transformer<R, MutableSet<V>> { observable ->
    observable.scan(mutableSetOf(), { prev, r ->
        prev.add(toValue(r))
        prev
    })
}

@SafeVarargs
fun <T, R> combine(vararg transformers: Transformer<T, out R>) = combine(asList(*transformers))

fun <T, R> combine(transformers: Collection<Transformer<T, out R>>) = Transformer<T, R> { observable ->
    val shared = observable.share()
    merge<R>(transformers.map({ shared.compose(it) }))
}

/**
 * This only makes sense for observables that have behavior-like behaviour.
 */
fun <T> latest(observable: Observable<T>): T {
    return observable.first().toBlocking().single()
}

fun <T> all(observable: Observable<T>): List<T> {
    return observable.toList().toBlocking().single()
}
