package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static rx.Observable.just;

public class MultiplexerShould {
    private boolean isUnsubscribedA = false;

    private final PublishSubject<Integer> subjectA = PublishSubject.create();
    private final PublishSubject<Integer> subjectB = PublishSubject.create();

    private final Map<String, Observable<Integer>> streams = ImmutableMap.of(
            "abc", subjectA.doOnUnsubscribe(() -> isUnsubscribedA = true),
            "def", subjectB
    );
    private final Multiplexer<Double, String, Integer> mux = Multiplexer.create(streams::get);

    @Test
    public void combine_upstreams_in_order_and_without_dupes() throws Exception {
        final double tag = 4.2;
        final Observable<Pair<Double, List<String>>> selection = just(Pair.of(tag, newArrayList("abc", "def")));
        final TestSubscriber<Pair<Double, List<Integer>>> sub = TestSubscriber.create();
        mux.call(selection).subscribe(sub);
        subjectA.onNext(1);
        subjectB.onNext(10);
        subjectA.onNext(2);
        subjectA.onNext(3);
        subjectB.onNext(20);
        subjectA.onCompleted();
        subjectB.onCompleted();
        sub.awaitTerminalEvent();

        assertThat(sub.getOnNextEvents(), contains(
                Pair.of(tag, newArrayList(1, 10)),
                Pair.of(tag, newArrayList(2, 10)),
                Pair.of(tag, newArrayList(3, 10)),
                Pair.of(tag, newArrayList(3, 20))
        ));
    }

    @Test
    public void switch_upstream_when_selection_changes() throws Exception {
        final double tagA = 4.2;
        final double tagB = 3.1;
        final PublishSubject<Pair<Double, List<String>>> selection = PublishSubject.create();
        final TestSubscriber<Pair<Double, List<Integer>>> sub = TestSubscriber.create();
        mux.call(selection).subscribe(sub);
        selection.onNext(Pair.of(tagA, newArrayList("abc")));
        subjectA.onNext(1);
        subjectA.onNext(2);
        selection.onNext(Pair.of(tagB, newArrayList("def")));
        subjectA.onNext(3);                     // This one should be skipped
        subjectB.onNext(10);
        subjectB.onNext(20);
        subjectA.onCompleted();
        subjectB.onCompleted();
        selection.onCompleted();
        sub.awaitTerminalEvent();

        assertThat(sub.getOnNextEvents(), contains(
                Pair.of(tagA, newArrayList(1)),
                Pair.of(tagA, newArrayList(2)),
                Pair.of(tagB, newArrayList(10)),
                Pair.of(tagB, newArrayList(20))
        ));
    }

    @Test
    public void unsubscribe_from_upstream_when_selection_changes() throws Exception {
        final double tagA = 4.2;
        final double tagB = 3.1;
        final PublishSubject<Pair<Double, List<String>>> selection = PublishSubject.create();
        final TestSubscriber<Pair<Double, List<Integer>>> sub = TestSubscriber.create();
        mux.call(selection).subscribe(sub);
        selection.onNext(Pair.of(tagA, newArrayList("abc")));
        subjectA.onNext(1);
        subjectA.onNext(2);
        selection.onNext(Pair.of(tagB, newArrayList("def")));

        assertThat(isUnsubscribedA, equalTo(true));
    }
}
