package io.quartic.eval.quarty

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.common.test.assertThrows
import io.quartic.eval.EvaluatorException
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.utils.runAndExpectToTimeout
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.quarty.model.QuartyRequest
import io.quartic.quarty.model.QuartyResponse
import io.quartic.quarty.model.QuartyResponse.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

class QuartyProxyShould {
    private val outbound = Channel<QuartyRequest>(UNLIMITED)
    private val events = Channel<Event<QuartyResponse>>(UNLIMITED)
    private val client = mock<WebsocketClient<QuartyRequest, QuartyResponse>> {
        on { outbound } doReturn outbound
        on { events } doReturn events
    }
    private val quarty = QuartyProxy(client)
    private val phaseBuilder = mock<PhaseBuilder<*>>()

    @Test
    fun do_nothing_until_connection_open() {
        runAndExpectToTimeout {
            quarty.request(phaseBuilder, mock())
        }

        assertTrue(outbound.isEmpty)    // Because we haven't got as far as sending the request
    }

    @Test
    fun send_request_to_quarty() {
        runOrTimeout {
            val expected = mock<QuartyRequest>()
            quartyIsConnected()
            quartyWillRespondWith(listOf(
                mock<Complete>()
            ))

            quarty.request(phaseBuilder, expected)

            assertThat(outbound.receive(), equalTo(expected))
        }
    }

    @Test
    fun log_quarty_logs_and_progress() {
        runOrTimeout {
            quartyIsConnected()
            quartyWillRespondWith(listOf(
                Log("noob", "hole"),
                Progress("yeah"),
                mock<Complete>()
            ))

            quarty.request(phaseBuilder, mock())

            inOrder(phaseBuilder) {
                runBlocking {
                    verify(phaseBuilder).log("noob", "hole")
                    verify(phaseBuilder).log("progress", "yeah")
                }
            }
        }
    }

    @Test
    fun return_complete_message() {
        runOrTimeout {
            val expected = mock<Complete>()
            quartyIsConnected()
            quartyWillRespondWith(listOf(
                expected
            ))

            val result = quarty.request(phaseBuilder, mock())

            assertThat(result, equalTo(expected))
        }
    }

    @Test
    fun throw_if_disconnected() {
        runOrTimeout {
            quartyIsConnected()
            quartyIsDisconnected()

            assertThrows<EvaluatorException> {
                runBlocking {
                    quarty.request(phaseBuilder, mock())
                }
            }
        }
    }

    @Test
    fun close_on_close() {
        runOrTimeout {
            quartyIsConnected()
            quarty.close()

            verify(client).close()
        }
    }

    @Test
    fun close_on_error() {
        runOrTimeout {
            quartyIsConnected()
            quartyIsDisconnected()

            try {
                quarty.request(phaseBuilder, mock())
            } catch (e: Exception) {}

            verify(client).close()
        }
    }

    private suspend fun quartyIsConnected() = events.send(Connected())
    private suspend fun quartyIsDisconnected() = events.send(Disconnected())
    private suspend fun quartyWillRespondWith(responses: List<QuartyResponse>) =
        responses.forEach { events.send(MessageReceived(it)) }
}
