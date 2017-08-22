package io.quartic.eval.qube

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.quartic.eval.model.QubeRequest
import io.quartic.eval.model.QubeRequest.Create
import io.quartic.eval.model.QubeRequest.Destroy
import io.quartic.eval.model.QubeResponse
import io.quartic.eval.model.QubeResponse.Error
import io.quartic.eval.model.QubeResponse.Ready
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.utils.runOrTimeout
import io.quartic.eval.utils.use
import io.quartic.eval.websocket.WebsocketClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class QubeProxyImplShould {
    private val toQube = Channel<QubeRequest>(2)
    private val fromQube = Channel<QubeResponse>(2)
    private var nextUuid = 100
    private val client = mock<WebsocketClient<QubeRequest, QubeResponse>> {
        on { toServer } doReturn toQube
        on { fromServer } doReturn fromQube
    }
    private val qube = QubeProxyImpl(client) { uuid(nextUuid++) }

    @Test
    fun generate_unique_uuids() {
        runOrTimeout {
            async(CommonPool) { qube.createContainer() }
            async(CommonPool) { qube.createContainer() }

            assertThat(toQube.receive().uuid, equalTo(uuid(100)))
            assertThat(toQube.receive().uuid, equalTo(uuid(101)))
        }
    }

    @Test
    fun return_hostname_if_qube_successfully_creates_container() {
        runOrTimeout {
            qubeShouldCreateAsync()

            val container = qube.createContainer()

            assertThat(container.hostname, equalTo("noob"))
        }
    }

    @Test
    fun throw_error_if_qube_fails_to_create_container() {
        runOrTimeout {
            qubeShouldFailToCreateAsync()

            assertThrowsQubeException("Badness occurred") { qube.createContainer() }
        }
    }

    @Test
    fun report_errors_after_creation() {
        runOrTimeout {
            qubeShouldCreateAsync()

            val container = qube.createContainer()

            fromQube.send(Error(uuid(100), "Oh dear me"))

            assertThat(container.errors.receive().message, equalTo("Oh dear me"))
        }
    }

    @Test
    fun route_errors_for_concurrent_containers() {
        runOrTimeout {
            qubeShouldCreateAsync(2)

            val containerA = qube.createContainer()
            val containerB = qube.createContainer()

            fromQube.send(Error(uuid(100), "Message 100"))
            fromQube.send(Error(uuid(101), "Message 101"))

            assertThat(containerA.errors.receive().message, equalTo("Message 100"))
            assertThat(containerB.errors.receive().message, equalTo("Message 101"))
        }
    }

    @Test
    fun not_block_on_channel_to_client() {
        runOrTimeout {
            qubeShouldCreateAsync(2)

            qube.createContainer()

            repeat(4) {
                fromQube.send(Error(uuid(100), "Yup"))
            }
            // Note the client doesn't attempt to receive the messages

            qube.createContainer()  // This will timeout with a shallow channel
        }
    }

    @Test
    fun not_block_on_channel_from_clients() {
        runOrTimeout {
            qubeShouldCreateAsync(4)

            // Note Qube doesn't attempt to receive the close messages, so will timeout with a shallow channel
            (0..3)
                .map { qube.createContainer() }
                .forEach { it.close() }
        }
    }

    @Test
    fun destroy_on_close() {
        runOrTimeout {
            qubeShouldCreateAsync()

            qube.createContainer().use {}   // Should autoclose

            assertThat(toQube.receive(), equalTo(Destroy(uuid(100)) as QubeRequest))
        }
    }

    @Test
    fun close_error_channel_on_close() {
        runOrTimeout {
            qubeShouldCreateAsync()

            val container = qube.createContainer()
            container.use {}   // Should autoclose

            fromQube.send(Error(uuid(100), "Oh dear me"))

            assertTrue(container.errors.isClosedForReceive)
        }
    }

    private fun qubeShouldCreateAsync(num: Int = 1) = async(CommonPool) {
        repeat(num) {
            val request = toQube.receive()
            if (request is Create) {
                fromQube.send(Ready(request.uuid, "noob"))
            }
        }
    }

    private fun qubeShouldFailToCreateAsync(num: Int = 1) = async(CommonPool) {
        repeat(num) {
            val request = toQube.receive()
            if (request is Create) {
                fromQube.send(Error(request.uuid, "Badness occurred"))
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

    private fun uuid(x: Int) = UUID(0, x.toLong())
}
