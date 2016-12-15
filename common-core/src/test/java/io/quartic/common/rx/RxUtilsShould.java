package io.quartic.common.rx;

import org.hamcrest.Matcher;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import java.util.List;

import static io.quartic.common.rx.RxUtils.likeBehavior;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class RxUtilsShould {

    @Test
    public void understand_publishsubject() throws Exception {
        final PublishSubject<Integer> subject = PublishSubject.create();
        assertBeforeDuringAfter(
                subject,
                subject,
                contains(1),
                empty(),
                empty()
        );
    }

    @Test
    public void understand_behaviorsubject() throws Exception {
        final BehaviorSubject<Integer> subject = BehaviorSubject.create();
        assertBeforeDuringAfter(
                subject,
                subject,
                contains(1),
                contains(1),
                empty()
        );
    }

    @Test
    public void model_behavior_correctly() throws Exception {
        final PublishSubject<Integer> subject = PublishSubject.create();
        assertBeforeDuringAfter(
                subject,
                subject.compose(likeBehavior()),
                contains(1),
                contains(1),
                empty()
        );
    }

    private void assertBeforeDuringAfter(
            Observer<Integer> observer,
            Observable<Integer> observable,
            Matcher<? super List<? extends Integer>> matcherBefore,
            Matcher<? super List<? extends Integer>> matcherDuring,
            Matcher<? super List<? extends Integer>> matcherAfter
    ) {
        final TestSubscriber<Integer> before = subscribe(observable);
        observer.onNext(1);
        final TestSubscriber<Integer> during = subscribe(observable);
        observer.onCompleted();
        final TestSubscriber<Integer> after = subscribe(observable);

        assertThat(collect(before), matcherBefore);
        assertThat(collect(during), matcherDuring);
        assertThat(collect(after), matcherAfter);
    }

    private List<Integer> collect(TestSubscriber<Integer> subscriber) {
        subscriber.awaitTerminalEvent();
        return subscriber.getOnNextEvents();
    }

    private TestSubscriber<Integer> subscribe(Observable<Integer> source) {
        final TestSubscriber<Integer> sub = TestSubscriber.create();
        source.subscribe(sub);
        return sub;
    }
}
