package io.quartic.common.rx;

import org.junit.Test;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static io.quartic.common.rx.SelectFrom.getLatestFrom;

public class SelectFromShould {
    static class Layer {
        private final String key;
        private final Observable<Integer> values;

        Layer(String key, Observable<Integer> values) {
            this.key = key;
            this.values = values;
        }

        public String key() {
            return key;
        }

        public Observable<Integer> values() {
            return values;
        }
    }

    @Test
    public void name() throws Exception {

        final PublishSubject<Layer> layers = PublishSubject.create();
        final PublishSubject<String> keys = PublishSubject.create();


        keys
                .compose(getLatestFrom(layers, Layer::key, Layer::values))
                .subscribe(x -> System.out.println("RESULT: " + x));


        final BehaviorSubject<Integer> foo = BehaviorSubject.create();
        final BehaviorSubject<Integer> bar = BehaviorSubject.create();
        final BehaviorSubject<Integer> baz = BehaviorSubject.create();

        layers.onNext(new Layer("foo", foo));

        keys.onNext("foo");
        foo.onNext(1);
        foo.onNext(2);

        layers.onNext(new Layer("bar", bar));

        foo.onNext(3);
        bar.onNext(10);
        bar.onNext(20);

        keys.onNext("bar");

        foo.onNext(4);
        foo.onNext(5);
        bar.onNext(30);

        keys.onNext("baz");

        layers.onNext(new Layer("baz", baz));

        baz.onNext(100);
        bar.onNext(40);

        keys.onNext("bar");
    }

}
