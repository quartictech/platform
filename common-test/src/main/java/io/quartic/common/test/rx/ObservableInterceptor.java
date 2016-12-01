package io.quartic.common.test.rx;

import rx.Observable;

import java.util.concurrent.atomic.AtomicBoolean;

import static rx.Observable.never;

public final class ObservableInterceptor<T> {
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private final Observable<T> observable;

    public static <T> ObservableInterceptor<T> create() {
        return new ObservableInterceptor<>(never());
    }

    public static <T> ObservableInterceptor<T> create(Observable<T> observable) {
        return new ObservableInterceptor<>(observable);
    }

    public ObservableInterceptor(Observable<T> observable) {
        this.observable = observable
                .doOnSubscribe(() -> subscribed.set(true))
                .doOnUnsubscribe(() -> unsubscribed.set(true));;
    }

    public boolean subscribed() {
        return subscribed.get();
    }

    public boolean unsubscribed() {
        return unsubscribed.get();
    }

    public Observable<T> observable() {
        return observable;
    }
}
