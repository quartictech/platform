package io.quartic.eval.quarty

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.QuartyMessage.*
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import kotlinx.coroutines.experimental.channels.Channel
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant

class QuartyProxyShould {
    private data class Foo(val x: Int)

    private val clock = mock<Clock>()
    private val events = Channel<Event<QuartyMessage>>(2)
    private val client = mock<WebsocketClient<Unit, QuartyMessage>> {
        on { events } doReturn events
    }
    private val quarty = QuartyProxy(client, clock, Foo::class)

    private val instantA = mock<Instant>()
    private val instantB = mock<Instant>()

    @Before
    fun before() {
        whenever(clock.instant())
            .thenReturn(instantA)
            .thenReturn(instantB)
    }

    @Test
    fun send_success_if_quarty_sends_result() {
        runOrTimeout {
            val result = Foo(15)

            quartyWillSend(listOf(
                Result(OBJECT_MAPPER.convertValue(result))
            ))

            assertThat(invokeQuarty(), equalTo(Success(
                emptyList(),
                result
            ) as QuartyResult<*>))
        }
    }

    @Test
    fun send_failure_if_quarty_sends_failure() {
        runOrTimeout {
            quartyWillSend(listOf(
                Error("Big problems")
            ))

            assertThat(invokeQuarty(), equalTo(Failure<Any>(
                emptyList(),
                "Big problems"
            ) as QuartyResult<*>))
        }
    }

    @Test
    fun include_other_messages_in_result() {
        runOrTimeout {
            quartyWillSend(listOf(
                Log("stdout", "Yeah"),
                Progress("Lovely time"),
                Error("Big problems")
            ))

            assertThat(invokeQuarty(), equalTo(Failure<Any>(
                listOf(
                    LogEvent("stdout", "Yeah", instantA),
                    LogEvent("progress", "Lovely time", instantB)
                ),
                "Big problems"
            ) as QuartyResult<*>))
        }
    }

    @Test
    fun return_internal_error_if_no_result_or_failure() {
        runOrTimeout {
            quartyWillSend(listOf(
                Log("stdout", "Yeah"),
                Progress("Lovely time")
                // No result or error here!
            ))
            quartyIsDisconnected()

            assertThat(invokeQuarty(), equalTo(InternalError<Any>(
                listOf(
                    LogEvent("stdout", "Yeah", instantA),
                    LogEvent("progress", "Lovely time", instantB)
                ),
                "No terminating message received"
            ) as QuartyResult<*>))
        }
    }

    @Test
    fun ignore_payload_for_success_messages_if_type_is_unit() {
        quarty.close()
        val quarty = QuartyProxy(client, clock, Unit::class)

        runOrTimeout {
            quartyWillSend(listOf(
                Result(null)
            ))

            assertThat(
                quarty.results.receive(),
                equalTo(Success(emptyList(), Unit) as QuartyResult<*>)
            )
        }
    }

    @Test
    fun return_internal_error_if_parsing_fails() {
        runOrTimeout {
            quartyWillSend(listOf(
                Log("stdout", "Yeah"),
                Result(mapOf("gibberish" to "alex")),
                Log("stdout", "Oh yeah")
            ))

            assertThat(invokeQuarty(), equalTo(InternalError<Any>(
                listOf(
                    LogEvent("stdout", "Yeah", instantA)
                    // Second log message not captured
                ),
                "Error invoking Quarty"
            ) as QuartyResult<*>))
        }
    }

    @Test
    fun close_websocket_when_completing_normally() {
        runOrTimeout {
            quartyWillSend(listOf(
                Error("Oh dear")
            ))

            invokeQuarty()

            verify(client).close()
        }
    }

    @Test
    fun close_websocket_when_something_bad_happened() {
        runOrTimeout {
            quartyWillSend(listOf(
                Result(mapOf("gibberish" to "alex"))    // Cause error to occur
            ))

            invokeQuarty()

            verify(client).close()
        }
    }

    @Test
    fun close_websocket_on_close() {
        runOrTimeout {
            quarty.close()

            verify(client).close()
        }
    }


    private suspend fun invokeQuarty() = quarty.results.receive()

    private suspend fun quartyWillSend(messages: List<QuartyMessage>) {
        messages.forEach { events.send(MessageReceived(it)) }
    }
    private suspend fun quartyIsConnected() = events.send(Connected())
    private suspend fun quartyIsDisconnected() = events.send(Disconnected())
}
