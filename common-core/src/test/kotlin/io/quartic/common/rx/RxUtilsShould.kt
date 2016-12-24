package io.quartic.common.rx

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.bechte.junit.runners.context.HierarchicalContextRunner
import io.quartic.common.test.rx.Interceptor
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import rx.Observable
import rx.Observable.Transformer
import rx.Observable.just
import rx.Observer
import rx.observers.TestSubscriber
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.*

@RunWith(HierarchicalContextRunner::class)
class RxUtilsShould {
    private interface Input
    private interface State
    private interface Output

    inner class LikeBehaviorShould {
        @Test
        fun understand_publishsubject() {
            val subject = PublishSubject.create<Int>()
            assertBeforeDuringAfter(
                    subject,
                    subject,
                    contains(1),
                    empty<Int>(),
                    empty<Int>()
            )
        }

        @Test
        fun understand_behaviorsubject() {
            val subject = BehaviorSubject.create<Int>()
            assertBeforeDuringAfter(
                    subject,
                    subject,
                    contains(1),
                    contains(1),
                    empty<Int>()
            )
        }

        @Test
        fun model_behavior_correctly() {
            val subject = PublishSubject.create<Int>()
            assertBeforeDuringAfter(
                    subject,
                    subject.compose(likeBehavior<Int>()),
                    contains(1),
                    contains(1),
                    empty<Int>()
            )
        }

        private fun assertBeforeDuringAfter(
                observer: Observer<Int>,
                observable: Observable<Int>,
                matcherBefore: Matcher<in List<Int>>,
                matcherDuring: Matcher<in List<Int>>,
                matcherAfter: Matcher<in List<Int>>
        ) {
            val before = subscribe(observable)
            observer.onNext(1)
            val during = subscribe(observable)
            observer.onCompleted()
            val after = subscribe(observable)

            assertThat(collect(before), matcherBefore)
            assertThat(collect(during), matcherDuring)
            assertThat(collect(after), matcherAfter)
        }

        private fun collect(subscriber: TestSubscriber<Int>): List<Int> {
            subscriber.awaitTerminalEvent()
            return subscriber.onNextEvents
        }

        private fun subscribe(source: Observable<Int>): TestSubscriber<Int> {
            val sub = TestSubscriber.create<Int>()
            source.subscribe(sub)
            return sub
        }
    }

    inner class LatestShould {
        @Test
        fun return_latest_item() {
            val subject = BehaviorSubject.create<Int>()
            subject.onNext(1)
            subject.onNext(2)

            assertThat(latest(subject), equalTo(2))
        }

        @Test(expected = NoSuchElementException::class)
        fun throw_if_input_is_terminated() {
            val subject = BehaviorSubject.create<Int>()
            subject.onNext(1)
            subject.onCompleted()

            latest(subject)
        }
    }

    inner class MealyShould {
        @Test
        fun wrap_callback_correctly() {
            val initialState = mock<State>()
            val nextState = mock<State>()
            val inputA = mock<Input>()
            val inputB = mock<Input>()
            val output = mock<Output>()

            val next = mock<(State?, Input?) -> StateAndOutput<State, Output>>()
            whenever(next.invoke(any(), any())).thenReturn(StateAndOutput(nextState, output))

            val sub = TestSubscriber.create<Output>()

            just(inputA, inputB)
                    .compose(mealy(initialState, next))
                    .subscribe(sub)

            sub.awaitTerminalEvent()
            verify(next)(initialState, inputA)
            verify(next)(nextState, inputB)
            assertThat(sub.onNextEvents, contains(output, output))
        }
    }

    inner class CombineShould {
        @Test
        fun merge_outputs_from_all_transformers() {
            val sub = TestSubscriber.create<Int>()
            just(1, 2, 3)
                    .compose(combine(
                            Transformer<Int, Int> { x -> x.map({ y -> y + 1 })},
                            Transformer<Int, Int> { x -> x.map({ y -> y * 10 })}
                    ))
                    .subscribe(sub)

            sub.awaitTerminalEvent()
            assertThat(sub.onNextEvents, containsInAnyOrder(2, 3, 4, 10, 20, 30))
        }

        @Test
        fun share_input() {
            val input = PublishSubject.create<Int>()
            val interceptor = Interceptor.create<Int>()

            input
                    .compose(interceptor)
                    .compose(combine(
                            Transformer<Int, Int> { x -> x.map({ y -> y + 1 })},
                            Transformer<Int, Int> { x -> x.map({ y -> y * 10 })}
                    ))
                    .subscribe()

            input.onNext(1)
            input.onNext(2)
            input.onNext(3)
            input.onCompleted()

            assertThat(interceptor.subscribeCount(), equalTo(1))
        }

        @Test
        fun unsubscribe_from_upstream_when_downstream_unsubscribes() {
            val interceptor = Interceptor.create<Int>()

            Observable.never<Int>()
                    .compose(interceptor)
                    .compose(combine(
                            Transformer<Int, Int> { x -> x },
                            Transformer<Int, Int> { x -> x }
                    ))
                    .subscribe()
                    .unsubscribe()

            assertThat(interceptor.unsubscribed(), equalTo(true))
        }
    }
}
