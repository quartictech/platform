package io.quartic.common.test.rx

import rx.Observable
import rx.Observable.Transformer
import java.util.concurrent.atomic.AtomicInteger

class Interceptor<T> : Transformer<T, T> {
    private val _subscribeCount = AtomicInteger(0)
    private val _unsubscribeCount = AtomicInteger(0)
    private val _values = mutableListOf<T>()

    val subscribed: Boolean get() = subscribeCount > 0
    val unsubscribed: Boolean get() = unsubscribeCount > 0
    val subscribeCount: Int get() = _subscribeCount.get()
    val unsubscribeCount: Int get() = _unsubscribeCount.get()
    val values: List<T> get() = _values.toList()

    override fun call(observable: Observable<T>): Observable<T> {
        return observable
                .doOnNext({ synchronized(_values) { _values.add(it) } })  // Note this will be weird if we have multiple subscribers
                .doOnSubscribe({ _subscribeCount.incrementAndGet() })
                .doOnUnsubscribe({ _unsubscribeCount.incrementAndGet() })
    }
}