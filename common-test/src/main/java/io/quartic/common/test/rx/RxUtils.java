package io.quartic.common.test.rx;

import rx.Observable;

import java.util.List;

public final class RxUtils {
    private RxUtils() {}

    public static <T> List<T> all(Observable<T> observable) {
        return observable.toList().toBlocking().single();
    }
}
