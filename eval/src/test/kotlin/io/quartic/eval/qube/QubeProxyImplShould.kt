package io.quartic.eval.qube

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeRequest.Create
import io.quartic.qube.api.QubeRequest.Destroy
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.qube.api.QubeResponse.Running
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.utils.runAndExpectToTimeout
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.utils.use
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.MessageReceived
import io.quartic.qube.api.model.ContainerSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class QubeProxyImplShould {
    private val outbound = Channel<QubeRequest>(2)
    private val events = Channel<Event<QubeResponse>>(2)
    private var nextUuid = 100
    private val client = mock<WebsocketClient<QubeRequest, QubeResponse>> {
        on { outbound } doReturn outbound
        on { events } doReturn events
    }

    private val containerSpec = ContainerSpec("noobout:1", listOf("true"))
    private val qube = QubeProxyImpl(client, containerSpec) { uuid(nextUuid++) }

    @Test
    fun generate_unique_uuids() {
        runOrTimeout {
            qubeIsConnected()

            async(CommonPool) { qube.createContainer() }
            async(CommonPool) { qube.createContainer() }

            assertThat(outbound.receive().name, equalTo(uuid(100)))
            assertThat(outbound.receive().name, equalTo(uuid(101)))
        }
    }

    @Test
    fun return_hostname_if_qube_successfully_creates_container() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()

            assertThat(container.hostname, equalTo("noob"))
        }
    }

    @Test
    fun throw_error_if_qube_fails_to_create_container() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillFailToCreateAsync()

            assertThrowsQubeException("Badness occurred") { qube.createContainer() }
        }
    }

    @Test
    fun create_container_with_correct_spec() {
        val containerId = uuid(100)
        runOrTimeout {
            qubeIsConnected()
            val requests = qubeWillCreateAsync()

            qube.createContainer()

            assertThat(requests.await()[0] as Create, equalTo(Create(containerId, containerSpec)))
        }
    }

    @Test
    fun report_errors_after_creation() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()

            events.send(MessageReceived(Terminated.Failed(uuid(100), "Oh dear me")))

            assertThat(container.errors.receive().message, equalTo("Oh dear me"))
        }
    }

    @Test
    fun route_errors_for_concurrent_containers() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync(2)

            val containerA = qube.createContainer()
            val containerB = qube.createContainer()

            events.send(MessageReceived(Terminated.Failed(uuid(100), "Message 100")))
            events.send(MessageReceived(Terminated.Failed(uuid(101), "Message 101")))

            assertThat(containerA.errors.receive().message, equalTo("Message 100"))
            assertThat(containerB.errors.receive().message, equalTo("Message 101"))
        }
    }

    @Test
    fun not_block_on_channel_to_client() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync(2)

            qube.createContainer()

            repeat(4) {
                events.send(MessageReceived(Terminated.Failed(uuid(100), "Yup")))
            }
            // Note the client doesn't attempt to receive the messages

            qube.createContainer()  // This will timeout with a shallow channel
        }
    }

    @Test
    fun not_block_on_channel_from_clients() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync(4)

            // Note Qube doesn't attempt to receive the close messages, so will timeout with a shallow channel
            (0..3)
                .map { qube.createContainer() }
                .forEach { it.close() }
        }
    }

    @Test
    fun destroy_on_close() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            qube.createContainer().use {}   // Should autoclose

            assertThat(outbound.receive(), equalTo(Destroy(uuid(100)) as QubeRequest))
        }
    }

    @Test
    fun be_idempotent_on_close() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()
            container.close()
            container.close()

            assertThat(outbound.receive(), equalTo(Destroy(uuid(100)) as QubeRequest))
        }

        runAndExpectToTimeout { outbound.receive() }
    }

    @Test
    fun close_error_channel_on_close() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()
            container.use {}   // Should autoclose

            events.send(MessageReceived(Terminated.Failed(uuid(100), "Oh dear me")))

            assertTrue(container.errors.isClosedForReceive)
        }
    }

    @Test
    fun ignore_requests_until_connected() {
        runOrTimeout {
            val job = async(CommonPool) {
                qube.createContainer()      // Blocks until connected
            }

            delay(50)

            assertFalse(job.isCompleted)    // Shouldn't be complete yet

            qubeIsConnected()
            qubeWillCreateAsync()

            job.join()                      // This should complete only if we process requests after connection
        }
    }

    @Test
    fun disconnect_reports_errors_for_everything_currently_in_flight() {
        runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()      // Active

            val job = async(CommonPool) {
                qube.createContainer()                  // Pending
            }

            delay(50)
            qubeIsDisconnected()
            job.join()

            container.errors.receive()                  // We get an error here
            assertTrue(job.isCompletedExceptionally)    // And here
        }
    }

    @Test
    fun disconnect_is_idempotent() {
        val container = runOrTimeout {
            qubeIsConnected()
            qubeWillCreateAsync()

            val container = qube.createContainer()      // Active

            qubeIsDisconnected()
            qubeIsConnected()
            qubeIsDisconnected()

            container.errors.receive()                  // We get an error here

            container
        }

        runAndExpectToTimeout { container.errors.receive() }    // But not a second time
    }

    private suspend fun qubeIsConnected() = events.send(Event.Connected())
    private suspend fun qubeIsDisconnected() = events.send(Event.Disconnected())

    private fun qubeWillCreateAsync(num: Int = 1): Deferred<List<QubeRequest>> = async(CommonPool) {
        (0 until num).map {
            val request = outbound.receive()
            if (request is Create) {
                events.send(MessageReceived(Running(request.name, "noob")))
            }
            request
        }
    }

    private fun qubeWillFailToCreateAsync(num: Int = 1) = async(CommonPool) {
        repeat(num) {
            val request = outbound.receive()
            if (request is Create) {
                events.send(MessageReceived(Terminated.Failed(request.name, "Badness occurred")))
            }
        }
    }

    // TODO - incorporate into assertThrows?
    private suspend fun assertThrowsQubeException(expectedMessage: String, block: suspend () -> Unit) {
        try {
            block()
            fail("Should have thrown")
        } catch (qe: QubeException) {
            assertThat(qe.message, equalTo(expectedMessage))
        } catch (t: Throwable) {
            fail("Incorrect exception")
        }
    }

    private fun uuid(x: Int) = UUID(0, x.toLong()).toString()
}
