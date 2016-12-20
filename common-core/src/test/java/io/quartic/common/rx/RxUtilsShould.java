package io.quartic.common.rx;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.common.test.rx.Interceptor;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observer;
import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.NoSuchElementException;

import static io.quartic.common.rx.RxUtils.combine;
import static io.quartic.common.rx.RxUtils.latest;
import static io.quartic.common.rx.RxUtils.likeBehavior;
import static io.quartic.common.rx.RxUtils.mealy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

@RunWith(HierarchicalContextRunner.class)
public class RxUtilsShould {
    private interface Input {}
    private interface State {}
    private interface Output {}

    public class LikeBehaviorShould {
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

    public class LatestShould {
        @Test
        public void return_latest_item() throws Exception {
            final BehaviorSubject<Integer> subject = BehaviorSubject.create();
            subject.onNext(1);
            subject.onNext(2);

            assertThat(latest(subject), equalTo(2));
        }

        @Test(expected = NoSuchElementException.class)
        public void throw_if_input_is_terminated() throws Exception {
            final BehaviorSubject<Integer> subject = BehaviorSubject.create();
            subject.onNext(1);
            subject.onCompleted();

            latest(subject);
        }
    }

    public class MealyShould {
        @Test
        public void wrap_callback_correctly() throws Exception {
            final State initialState = mock(State.class);
            final Input inputA = mock(Input.class);
            final Input inputB = mock(Input.class);
            final State nextState = mock(State.class);
            final Output output = mock(Output.class);

            @SuppressWarnings("unchecked") Func2<State, Input, StateAndOutput<State, Output>> next = mock(Func2.class);
            when(next.call(any(), any())).thenReturn(StateAndOutput.of(nextState, output));

            TestSubscriber<Output> sub = TestSubscriber.create();

            just(inputA, inputB)
                    .compose(mealy(initialState, next))
                    .subscribe(sub);

            sub.awaitTerminalEvent();
            verify(next).call(initialState, inputA);
            verify(next).call(nextState, inputB);
            assertThat(sub.getOnNextEvents(), contains(output, output));
        }
    }

    public class CombineShould {
        @Test
        public void merge_outputs_from_all_transformers() throws Exception {
            final TestSubscriber<Integer> sub = TestSubscriber.create();
            just(1, 2, 3)
                    .compose(combine(
                            x -> x.map(y -> y + 1),
                            x -> x.map(y -> y * 10)
                    ))
                    .subscribe(sub);

            sub.awaitTerminalEvent();
            assertThat(sub.getOnNextEvents(), containsInAnyOrder(2, 3, 4, 10, 20, 30));
        }

        @Test
        public void share_input() throws Exception {
            final PublishSubject<Integer> input = PublishSubject.create();
            final Interceptor<Integer> interceptor = Interceptor.create();

            input
                    .compose(interceptor)
                    .compose(combine(
                            x -> x.map(y -> y + 1),
                            x -> x.map(y -> y * 10)
                    ))
                    .subscribe();

            input.onNext(1);
            input.onNext(2);
            input.onNext(3);
            input.onCompleted();

            assertThat(interceptor.subscribeCount(), equalTo(1));
        }

        @Test
        public void unsubscribe_from_upstream_when_downstream_unsubscribes() throws Exception {
            final Interceptor<Integer> interceptor = Interceptor.create();

            Observable.<Integer>never()
                    .compose(interceptor)
                    .compose(combine(x -> x, x -> x))
                    .subscribe()
                    .unsubscribe();

            assertThat(interceptor.unsubscribed(), equalTo(true));
        }
    }
}
