package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
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
    private final Multiplexer<String, Integer> mux = Multiplexer.create(streams::get);

    @Test
    public void combine_upstreams_in_order_and_without_dupes() throws Exception {
        final TestSubscriber<List<Integer>> sub = TestSubscriber.create();
        mux.multiplex(just(newArrayList("abc", "def"))).subscribe(sub);
        subjectA.onNext(1);
        subjectB.onNext(10);
        subjectA.onNext(2);
        subjectA.onNext(3);
        subjectB.onNext(20);
        subjectA.onCompleted();
        subjectB.onCompleted();
        sub.awaitTerminalEvent();

        assertThat(sub.getOnNextEvents(), contains(
                newArrayList(1, 10),
                newArrayList(2, 10),
                newArrayList(3, 10),
                newArrayList(3, 20)
        ));
    }

    @Test
    public void switch_upstream_when_selection_changes() throws Exception {
        final PublishSubject<List<String>> selection = PublishSubject.create();
        final TestSubscriber<List<Integer>> sub = TestSubscriber.create();
        mux.multiplex(selection).subscribe(sub);
        selection.onNext(newArrayList("abc"));
        subjectA.onNext(1);
        subjectA.onNext(2);
        selection.onNext(newArrayList("def"));
        subjectA.onNext(3);                     // This one should be skipped
        subjectB.onNext(10);
        subjectB.onNext(20);
        subjectA.onCompleted();
        subjectB.onCompleted();
        selection.onCompleted();
        sub.awaitTerminalEvent();

        assertThat(sub.getOnNextEvents(), contains(
                newArrayList(1),
                newArrayList(2),
                newArrayList(10),
                newArrayList(20)
        ));
    }

    @Test
    public void unsubscribe_from_upstream_when_selection_changes() throws Exception {
        final PublishSubject<List<String>> selection = PublishSubject.create();
        final TestSubscriber<List<Integer>> sub = TestSubscriber.create();
        mux.multiplex(selection).subscribe(sub);
        selection.onNext(newArrayList("abc"));
        subjectA.onNext(1);
        subjectA.onNext(2);
        selection.onNext(newArrayList("def"));

        assertThat(isUnsubscribedA, equalTo(true));
    }
}
