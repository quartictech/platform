package io.quartic.eval.qube

import com.google.common.base.Preconditions.checkState
import io.quartic.common.logging.logger
import io.quartic.qube.api.QubeResponse.Running
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Create
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Destroy
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.model.ContainerSpec
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
    private val container: ContainerSpec,
    private val nextUuid: () -> QubeId = { UUID.randomUUID().toString() }
) : QubeProxy {
    private sealed class ClientRequest {
        data class Create(val response: CompletableDeferred<QubeContainerProxy> = CompletableDeferred()) : ClientRequest()
        data class Destroy(val uuid: QubeId) : ClientRequest()
    }

    private var connected = false
    private val pending = mutableMapOf<QubeId, Create>()
    private val active = mutableMapOf<QubeId, SendChannel<QubeException>>()
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
        qube.outbound.send(QubeRequest.Create(uuid, container))
    }

    private suspend fun handleDestroyRequest(request: Destroy) {
        LOG.info("[${request.uuid}] -> DESTROY")

        val pending = pending.remove(request.uuid)
        val active = active.remove(request.uuid)
        // These checks ensure this method is idempotent, i.e. we don't send multiple requests to Qube.
        // This is important because this method is likely to be called after a reconnect (where we already killed everything)
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
        is Running -> handleRunningResponse(response)
        is Terminated -> handleTerminatedResponse(response)
        else -> LOG.info("[{}] Ignoring response: {}", response.name, response)
    }

    private fun handleRunningResponse(response: Running) {
        LOG.info("[${response.name}] <- READY")

        val pending = pending.remove(response.name)
        when {
            (response.name in active) ->
                LOG.error("Ready response duplicated")
            (pending == null) ->
                LOG.error("Ready response doesn't correspond to pending request")
            else -> {
                val channel = Channel<QubeException>(UNLIMITED)
                active[response.name] = channel
                pending.response.complete(QubeContainerProxy(response.hostname, channel) {
                    fromClients.send(Destroy(response.name))
                    channel.close()
                })
            }
        }
    }

    private suspend fun handleTerminatedResponse(response: Terminated) {
        LOG.info("[${response.name}] <- ERROR")

        val pending = pending.remove(response.name)
        val active = active[response.name]
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
