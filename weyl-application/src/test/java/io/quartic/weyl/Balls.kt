package io.quartic.weyl

import org.junit.Test
import rx.Observable
import rx.subjects.BehaviorSubject

class Balls {
    @Test
    fun name() {
        val observable : Observable<Int> = Observable.just(1, 2, 3)
        val subject = BehaviorSubject.create<Int>()
        observable.subscribe(subject)
        subject.subscribe{
            result ->
            System.out.println("Start $result in Subscription Result")
        }
    }
}
