package io.quartic.eval.websocket

import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.websocket.WebsocketServerRule
import io.quartic.eval.utils.runOrTimeout
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class WebsocketClientShould {
    @get:Rule
    val server = WebsocketServerRule()

    data class SendMsg(val s: String)
    data class ReceiveMsg(val i: Int)

    // TODO - test websocket closure

    @Test
    fun receive_messages() {
        val expected = listOf(
            ReceiveMsg(42),
            ReceiveMsg(43),
            ReceiveMsg(44)
        )

        serverShouldSend(expected)

        val client = createClient()

        runOrTimeout {
            assertThat(client.collectReceivedMessages(3), equalTo(expected))
        }
    }

    @Test
    fun send_messages() {
        val expected = listOf(
            SendMsg("alice"),
            SendMsg("bob"),
            SendMsg("charlie")
        )

        val client = createClient()

        runOrTimeout {
            expected.forEach {
                client.toServer.send(it)
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

        val client = createClient()

        runOrTimeout {
            assertThat(client.collectReceivedMessages(2), equalTo(listOf(expected[0], expected[2])))
        }
    }

    @Test
    @Ignore
    fun lafjhs() {
        val client = createClient()

        Thread.sleep(500)

        server.dropConnections()

//        Thread.sleep(30000)
        runBlocking {
//        runOrTimeout {
            repeat(1000) {
                client.toServer.send(SendMsg("noob"))
                delay(100)
            }
        }
    }

    private fun serverShouldSend(messages: List<Any>) {
        server.messages = messages.map { OBJECT_MAPPER.writeValueAsString(it) }
    }

    private fun createClient() = WebsocketClient.create<SendMsg, ReceiveMsg>(server.uri)

    private suspend fun WebsocketClient<SendMsg, ReceiveMsg>.collectReceivedMessages(num: Int): List<ReceiveMsg> {
        val received = mutableListOf<ReceiveMsg>()
        repeat(num) {
            received.add(fromServer.receive())
        }
        return received
    }
}
