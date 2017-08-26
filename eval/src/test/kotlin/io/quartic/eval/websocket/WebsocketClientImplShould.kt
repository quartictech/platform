package io.quartic.eval.websocket

import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.websocket.WebsocketServerRule
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.websocket.WebsocketClient.Event.*
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class WebsocketClientImplShould {
    @get:Rule
    val server = WebsocketServerRule()

    data class SendMsg(val s: String)
    data class ReceiveMsg(val i: Int)

    // TODO - bias toward internal events

    @Test
    fun receive_messages() {
        val expected = listOf(
            ReceiveMsg(42),
            ReceiveMsg(43),
            ReceiveMsg(44)
        )
        serverShouldSend(expected)

        createClient().use { client ->
            runOrTimeout {
                client.awaitConnected()

                assertThat(client.awaitReceivedMessages(3), equalTo(expected))
            }
        }
    }

    @Test
    fun send_messages() {
        val expected = listOf(
            SendMsg("alice"),
            SendMsg("bob"),
            SendMsg("charlie")
        )

        createClient().use { client ->
            runOrTimeout {
                client.awaitConnected()

                expected.forEach {
                    client.outbound.send(it)
                }
            }
        }

        Thread.sleep(100)       // TODO: this is gross
        assertThat(server.receivedMessages, equalTo(expected.map { OBJECT_MAPPER.writeValueAsString(it) }))
    }

    @Test
    fun not_die_when_receiving_malformed_message() {
        val expected = listOf(
            ReceiveMsg(42),
            "gibberish",
            ReceiveMsg(44)
        )
        serverShouldSend(expected)

        createClient().use { client ->
            runOrTimeout {
                client.awaitConnected()

                assertThat(client.awaitReceivedMessages(2), equalTo(listOf(expected[0], expected[2])))
            }
        }
    }

    @Test
    fun notify_consumer_when_connection_dropped() {
        createClient().use { client ->
            runOrTimeout {
                client.awaitConnected()

                server.dropConnections()

                client.awaitDisconnected()
            }
        }
    }

    @Test
    fun reconnect_then_continue_working_when_connection_dropped() {
        val expected = listOf(ReceiveMsg(42))
        serverShouldSend(expected)

        createClient().use { client ->
            runOrTimeout(1000) {
                client.awaitConnected()

                server.dropConnections()

                client.awaitConnected()

                assertThat(client.awaitReceivedMessages(1), equalTo(expected))
            }
        }
    }

    @Test
    fun drop_outbound_messages_whilst_connection_dropped() {
        createClient().use { client ->
            runOrTimeout {
                client.awaitConnected()

                server.dropConnections()

                client.awaitDisconnected()

                client.outbound.send(SendMsg("no"))
            }
        }

        Thread.sleep(100)
        assertThat(server.receivedMessages, hasSize(0))
    }

    @Ignore("This is extremely flaky on CI, given we're trying to verify something *doesn't* happen asynchronously")
    @Test
    fun disconnect_and_not_attempt_reconnect_on_close() {
        val client = createClient()
        try {
            runOrTimeout {
                client.awaitConnected()
                client.close()
                client.awaitDisconnected()  // Should timeout, as we don't send message
            }
        } catch (e: Exception) {}           // We're expecting a timeout

        Thread.sleep(100)
        assertThat(server.numConnections, equalTo(1))
        assertThat(server.numDisconnections, equalTo(1))
    }

    @Test
    fun close_gracefully_if_not_yet_connected() {
        server.stop()   // Prevent connections

        val client = createClient()
        client.close()  // Should be no exceptions
    }

    @Test
    fun close_channels_on_close() {
        val client = createClient()
        client.close()

        runOrTimeout {
            assertThat(client.events.receiveOrNull(), nullValue())
            assertTrue(client.outbound.isClosedForSend)
        }
    }

    private fun serverShouldSend(messages: List<Any>) {
        server.messages = messages.map { OBJECT_MAPPER.writeValueAsString(it) }
    }

    private fun createClient() = WebsocketClientImpl.create<SendMsg, ReceiveMsg>(server.uri, Duration.ofMillis(100))

    private suspend fun WebsocketClient<SendMsg, ReceiveMsg>.awaitReceivedMessages(num: Int): List<ReceiveMsg> {
        val received = mutableListOf<ReceiveMsg>()
        repeat(num) {
            val event = events.receive()
            if (event is MessageReceived) {
                received.add(event.message)
            }
        }
        return received
    }

    private suspend fun WebsocketClient<SendMsg, ReceiveMsg>.awaitConnected() = await(Connected::class.java)
    private suspend fun WebsocketClient<SendMsg, ReceiveMsg>.awaitDisconnected() = await(Disconnected::class.java)

    private suspend fun <T : WebsocketClient.Event<*>> WebsocketClient<SendMsg, ReceiveMsg>.await(clazz: Class<T>) {
        while (!clazz.isInstance(events.receive())) {}
    }

}

