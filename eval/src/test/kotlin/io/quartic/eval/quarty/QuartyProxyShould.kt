package io.quartic.eval.quarty

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.common.auth.internal.InternalTokenGenerator
import io.quartic.common.auth.internal.InternalUser
import io.quartic.common.test.assertThrows
import io.quartic.eval.EvaluatorException
import io.quartic.eval.utils.runAndExpectToTimeout
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.quarty.api.model.QuartyAuthenticatedRequest
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyResponse
import io.quartic.quarty.api.model.QuartyResponse.*
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

class QuartyProxyShould {
    private val namespace = "my-namespace"
    private val token = "super-secret"

    private val outbound = Channel<QuartyAuthenticatedRequest>(UNLIMITED)
    private val events = Channel<Event<QuartyResponse>>(UNLIMITED)
    private val client = mock<WebsocketClient<QuartyAuthenticatedRequest, QuartyResponse>> {
        on { outbound } doReturn outbound
        on { events } doReturn events
    }
    private val customer = mock<Customer> {
        on { namespace } doReturn namespace
    }
    private val tokenGen = mock<InternalTokenGenerator> {
        on { generate(InternalUser(namespace, listOf(namespace))) } doReturn token
    }
    private val quarty = QuartyProxy(customer, tokenGen, client)
    private val log = mock<(String, String) -> Unit>()

    @Test
    fun do_nothing_until_connection_open() {
        runAndExpectToTimeout {
            quarty.request(mock(), log)
        }

        assertTrue(outbound.isEmpty)    // Because we haven't got as far as sending the request
    }

    @Test
    fun send_authenticated_request_to_quarty() {
        runOrTimeout {
            val expected = mock<QuartyRequest>()
            quartyIsConnected()
            quartyWillRespondWith(listOf(
                mock<Complete>()
            ))

            quarty.request(expected, log)

            assertThat(outbound.receive(), equalTo(QuartyAuthenticatedRequest(token, expected)))
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

            quarty.request(mock(), log)

            inOrder(log) {
                verify(log)("noob", "hole")
                verify(log)("progress", "yeah")
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

            val result = quarty.request(mock(), log)

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
                    quarty.request(mock(), log)
                }
            }
        }
    }

    @Test
    fun throw_if_previously_disconnected() {
        runOrTimeout {
            quartyIsConnected()
            quartyIsDisconnected()

            try {
                quarty.request(mock(), log)
            } catch (e: Exception) {}   // We already know the first one fails
        }
    }

    @Test
    fun throw_if_previously_aborted() {
        runOrTimeout {
            quartyIsAborted()

            assertThrows<EvaluatorException> {
                runBlocking {
                    quarty.request(mock(), log)
                }
            }
        }
    }

    @Test
    fun close_on_close() {
        runOrTimeout {
            quartyIsConnected()
            quarty.close()
        }

        Thread.sleep(100)   // Gross

        verify(client).close()
    }

    private suspend fun quartyIsConnected() = events.send(Connected())
    private suspend fun quartyIsDisconnected() = events.send(Disconnected())
    private suspend fun quartyIsAborted() = events.send(Aborted())
    private suspend fun quartyWillRespondWith(responses: List<QuartyResponse>) =
        responses.forEach { events.send(MessageReceived(it)) }
}
