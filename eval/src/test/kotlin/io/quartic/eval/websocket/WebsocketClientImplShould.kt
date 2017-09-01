package io.quartic.eval.websocket

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.eval.websocket.WebsocketFactory.Websocket
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class WebsocketClientImplShould {
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
        server.willSend(expected)

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

        assertThat(server.awaitReceivedMessages(3), equalTo(expected.map { OBJECT_MAPPER.writeValueAsString(it) }))
    }

    @Test
    fun not_die_when_receiving_malformed_message() {
        val expected = listOf(
            ReceiveMsg(42),
            "gibberish",
            ReceiveMsg(44)
        )
        server.willSend(expected)

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
        server.willSend(expected)

        createClient().use { client ->
            runOrTimeout {
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

        assertThrows<CancellationException> {
            server.awaitReceivedMessages(1)
        }
    }

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

        runOrTimeout {
            server.awaitDisconnection()
            assertThat(server.numConnections, equalTo(1))
        }
    }

    @Test
    fun close_gracefully_if_not_yet_connected() {
        server.refuseConnections()

        val client = createClient()
        client.close()  // Should be no exceptions
    }

    @Test
    fun close_channels_on_close() {
        val client = createClient()
        runOrTimeout {
            client.events.receive() // Connection event
            client.close()
            assertThat(client.events.receiveOrNull(), nullValue())
            assertTrue(client.outbound.isClosedForSend)
        }
    }

    private fun createClient() = WebsocketClientImpl.create<SendMsg, ReceiveMsg>(mock(), Duration.ofMillis(100), server.websocketFactory)

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


    private class Server {
        private var messagesToSend = emptyList<String>()
        private var _numConnections = 0
        private var acceptingConnections = true
        private val rx = Channel<String>(Channel.UNLIMITED)
        private val disconnections = Channel<Unit>(Channel.UNLIMITED)

        private val websocket = mock<Websocket> {
            on { writeTextMessage(any()) } doAnswer { invocation ->
                runBlocking { rx.send(invocation.getArgument<String>(0)) }
                Unit
            }
        }
        private lateinit var closeHandler: () -> Unit

        fun willSend(messages: List<Any>) {
            messagesToSend = messages.map { OBJECT_MAPPER.writeValueAsString(it) }
        }

        fun dropConnections() {
            closeHandler()
        }

        fun refuseConnections() {
            acceptingConnections = false
        }

        fun awaitReceivedMessages(num: Int) = runOrTimeout { (0 until num).map { rx.receive() } }
        fun awaitDisconnection() = runOrTimeout { disconnections.receive() }

        val numConnections get() = _numConnections

        val websocketFactory = mock<WebsocketFactory> {
            on { create(any(), any(), any(), any(), any()) } doAnswer { invocation ->
                val connectHandler = invocation.getArgument<(Websocket) -> Unit>(1)
                val failureHandler = invocation.getArgument<(Throwable) -> Unit>(2)
                val messageHandler = invocation.getArgument<(String) -> Unit>(3)

                if (acceptingConnections) {
                    closeHandler = invocation.getArgument(4)
                    connectHandler(websocket)
                    messagesToSend.forEach(messageHandler)
                    _numConnections++
                } else {
                    failureHandler(RuntimeException("Not accepting connections"))
                }
                Unit
            }
            on { close() } doAnswer {
                runBlocking { disconnections.send(Unit) }
                Unit
            }
        }
    }

    private val server = Server()
}

