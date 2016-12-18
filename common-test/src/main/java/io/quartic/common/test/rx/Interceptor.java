package io.quartic.common.test.rx;

import com.google.common.collect.ImmutableList;
import rx.Observable;
import rx.Observable.Transformer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.synchronizedList;

public final class Interceptor<T> implements Transformer<T, T> {
    private final AtomicInteger subscribeCount = new AtomicInteger(0);
    private final AtomicInteger unsubscribeCount = new AtomicInteger(0);
    private final List<T> values = synchronizedList(newArrayList());

    public static <T> Interceptor<T> create() {
        return new Interceptor<>();
    }

    public boolean subscribed() {
        return subscribeCount.get() > 0;
    }

    public boolean unsubscribed() {
        return unsubscribeCount.get() > 0;
    }

    public int subscribeCount() {
        return subscribeCount.get();
    }

    public int unsubscribeCount() {
        return unsubscribeCount.get();
    }

    public List<T> values() {
        return ImmutableList.copyOf(values);
    }

    @Override
    public Observable<T> call(Observable<T> observable) {
        return observable
                .doOnNext(values::add)  // Note this will be weird if we have multiple subscribers
                .doOnSubscribe(subscribeCount::getAndIncrement)
                .doOnUnsubscribe(unsubscribeCount::getAndIncrement);
    }
}
