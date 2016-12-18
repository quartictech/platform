package io.quartic.common.rx;

import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.common.test.rx.Interceptor;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observable.Transformer;
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
import static rx.Observable.never;

@RunWith(Enclosed.class)
public class RxUtilsShould {

    public static class LikeBehaviorShould {
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

    public static class LatestShould {
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

    public static class MealyShould {
        interface Input {}
        interface State {}
        interface Output {}

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

    public static class CombineShould {
        interface Input {}
        interface Output {}

        @Test
        public void merge_outputs_from_all_transformers() throws Exception {
            final Input inputA = mock(Input.class);
            final Input inputB = mock(Input.class);
            final Output outputA = mock(Output.class);
            final Output outputB = mock(Output.class);

            final TestSubscriber<Input> subInput1 = TestSubscriber.create();
            final TestSubscriber<Input> subInput2 = TestSubscriber.create();
            final Transformer<Input, Output> t1 = mockTransformer(subInput1, just(outputA));
            final Transformer<Input, Output> t2 = mockTransformer(subInput2, just(outputB));

            TestSubscriber<Output> sub = TestSubscriber.create();

            just(inputA, inputB)
                    .compose(combine(t1, t2))
                    .subscribe(sub);

            sub.awaitTerminalEvent();
            assertThat(subInput1.getOnNextEvents(), contains(inputA, inputB));
            assertThat(subInput2.getOnNextEvents(), contains(inputA, inputB));
            assertThat(sub.getOnNextEvents(), containsInAnyOrder(outputA, outputB));
        }

        @Test
        public void share_input() throws Exception {
            final Transformer<Input, Output> t1 = mockTransformer(TestSubscriber.create(), never());
            final Transformer<Input, Output> t2 = mockTransformer(TestSubscriber.create(), never());

            final Interceptor<Input> interceptor = Interceptor.create();

            Observable.<Input>never()
                    .compose(interceptor)
                    .compose(combine(t1, t2))
                    .subscribe();

            assertThat(interceptor.subscribeCount(), equalTo(1));
        }

        @Test
        public void unsubscribe_from_upstream_when_downstream_unsubscribes() throws Exception {
            final Interceptor<Input> interceptor = Interceptor.create();

            Observable.<Input>never()
                    .compose(interceptor)
                    .compose(combine(x -> x, x -> x))
                    .subscribe()
                    .unsubscribe();

            assertThat(interceptor.unsubscribed(), equalTo(true));
        }

        private <T, R> Transformer<T, R> mockTransformer(TestSubscriber<T> testSubscriber, Observable<R> output) {
            @SuppressWarnings("unchecked") final Transformer<T, R> transformer = mock(Transformer.class);
            when(transformer.call(any())).thenAnswer(invocation -> {
                Observable<T> input = invocation.getArgument(0);
                input.subscribe(testSubscriber);
                return output;
            });
            return transformer;
        }
    }
}
