package io.quartic.common.websocket

import io.quartic.common.serdes.encode
import io.quartic.common.test.websocket.WebsocketServerRule
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit.MILLISECONDS

class WebsocketListenerShould {

    @get:Rule
    val server = WebsocketServerRule()

    val listener by lazy {
        WebsocketListener.Factory(server.uri, WebsocketClientSessionFactory(javaClass)).create(TestThing::class.java)
    }

    @Test
    fun emit_items_from_socket() {
        server.messages = listOf(encode(TestThing("foo")), encode(TestThing("bar")))

        val subscriber = TestSubscriber.create<TestThing>()
        listener.observable.subscribe(subscriber)
        subscriber.awaitValueCount(2, TIMEOUT_MILLISECONDS, MILLISECONDS)

        assertThat(subscriber.onNextEvents, contains(TestThing("foo"), TestThing("bar")))
    }

    @Test
    fun skip_undecodable_items() {
        server.messages = listOf("bad", encode(TestThing("bar")))

        val subscriber = TestSubscriber.create<TestThing>()
        listener.observable.subscribe(subscriber)
        subscriber.awaitValueCount(1, TIMEOUT_MILLISECONDS, MILLISECONDS)

        assertThat(subscriber.onNextEvents, contains(TestThing("bar")))
    }

    @Test
    fun only_create_one_connection_if_multiple_subscribers() {
        val subA = TestSubscriber.create<TestThing>()
        val subB = TestSubscriber.create<TestThing>()
        listener.observable.subscribe(subA)
        listener.observable.subscribe(subB)

        assertThat(server.numConnections, equalTo(1))
    }

    @Test
    fun not_connect_to_websocket_if_no_subscribers() {
        assertThat(server.numConnections, equalTo(0))
    }

    @Test
    fun close_socket_on_unsubscribe() {
        listener.observable.subscribe().unsubscribe()

        assertThat(server.numDisconnections, equalTo(1))
    }

    @Test
    fun not_close_socket_if_only_one_of_two_subscribers_unsubscribes() {
        listener.observable.subscribe()
        listener.observable.subscribe().unsubscribe()

        assertThat(server.numDisconnections, equalTo(0))
    }

    data class TestThing(val name: String)

    companion object {
        private val TIMEOUT_MILLISECONDS: Long = 1000
    }
}
