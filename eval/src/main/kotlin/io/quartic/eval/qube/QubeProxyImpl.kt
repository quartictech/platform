package io.quartic.eval.qube

import com.google.common.base.Preconditions.checkState
import io.quartic.common.logging.logger
import io.quartic.eval.model.QubeRequest
import io.quartic.eval.model.QubeResponse
import io.quartic.eval.model.QubeResponse.Error
import io.quartic.eval.model.QubeResponse.Ready
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Create
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Destroy
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import java.util.*

class QubeProxyImpl(
    private val qube: WebsocketClient<QubeRequest, QubeResponse>,
    private val nextUuid: () -> UUID = UUID::randomUUID
) : QubeProxy {
    private sealed class ClientRequest {
        data class Create(val response: CompletableDeferred<QubeContainerProxy> = CompletableDeferred()) : ClientRequest()
        data class Destroy(val uuid: UUID) : ClientRequest()
    }

    private var connected = false
    private val pending = mutableMapOf<UUID, Create>()
    private val active = mutableMapOf<UUID, SendChannel<QubeException>>()
    private val fromClients = Channel<ClientRequest>(UNLIMITED)
    private val LOG by logger()

    init {
        // TODO - where should we do this?
        launch(CommonPool) {
            runEventLoop()
        }
    }

    override suspend fun createContainer() = with (Create()) {
        fromClients.send(this)
        response.await()
    }

    private suspend fun runEventLoop() {
        while (true) {
            select<Unit> {
                if (connected) {
                    fromClients.onReceive {
                        when (it) {
                            is Create -> handleCreateRequest(it)
                            is Destroy -> handleDestroyRequest(it)
                        }
                    }
                }

                qube.events.onReceive {
                    when (it) {
                        is Connected -> handleConnected()
                        is Disconnected -> handleDisconnected()
                        is MessageReceived -> handleResponse(it.message)
                    }
                }
            }
        }
    }

    private suspend fun handleCreateRequest(request: Create) {
        val uuid = nextUuid()
        LOG.info("[$uuid] -> CREATE")

        pending[uuid] = request
        qube.outbound.send(QubeRequest.Create(uuid))
    }

    private suspend fun handleDestroyRequest(request: Destroy) {
        LOG.info("[${request.uuid}] -> DESTROY")

        val pending = pending.remove(request.uuid)
        val active = active.remove(request.uuid)
        // Check ensures idempotency, which is also important post-reconnect (because we already killed everything)
        if (pending != null || active != null) {
            qube.outbound.send(QubeRequest.Destroy(request.uuid))
        }
    }

    private suspend fun handleConnected() {
        checkState(!connected, "Already connected")
        connected = true
    }

    private suspend fun handleDisconnected() {
        checkState(connected, "Already disconnected")
        connected = false

        // Kill everything
        val exception = QubeException("Qube disconnected")
        pending.values.forEach { it.response.completeExceptionally(exception) }
        active.values.forEach { it.send(exception) }    // Client will call close on corresponding QubeContainerProxy, but that's ok
        pending.clear()
        active.clear()
    }

    private suspend fun handleResponse(response: QubeResponse) = when (response) {
        is Ready -> handleReadyResponse(response)
        is Error -> handleErrorResponse(response)
    }

    private fun handleReadyResponse(response: Ready) {
        LOG.info("[${response.uuid}] <- READY")

        val pending = pending.remove(response.uuid)
        when {
            (response.uuid in active) ->
                LOG.error("Ready response duplicated")
            (pending == null) ->
                LOG.error("Ready response doesn't correspond to pending request")
            else -> {
                val channel = Channel<QubeException>(UNLIMITED)
                active[response.uuid] = channel
                pending.response.complete(QubeContainerProxy(response.hostname, channel) {
                    fromClients.send(Destroy(response.uuid))
                    channel.close()
                })
            }
        }
    }

    private suspend fun handleErrorResponse(response: Error) {
        LOG.info("[${response.uuid}] <- ERROR")

        val pending = pending.remove(response.uuid)
        val active = active[response.uuid]
        val exception = QubeException(response.message)

        when {
            (pending != null) ->
                pending.response.completeExceptionally(exception)
            (active != null) ->
                active.send(exception)
            else ->
                LOG.error("Error response doesn't correspond to pending request or active container")
        }
    }
}
