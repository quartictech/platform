package io.quartic.eval.qube

import io.quartic.common.logging.logger
import io.quartic.eval.model.QubeRequest
import io.quartic.eval.model.QubeResponse
import io.quartic.eval.model.QubeResponse.Error
import io.quartic.eval.model.QubeResponse.Ready
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Create
import io.quartic.eval.qube.QubeProxyImpl.ClientRequest.Destroy
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import java.util.*

class QubeProxyImpl(
    private val toQube: SendChannel<QubeRequest>,
    private val fromQube: ReceiveChannel<QubeResponse>,
    private val nextUuid: () -> UUID = UUID::randomUUID
) : QubeProxy {
    private sealed class ClientRequest {
        data class Create(val response: CompletableDeferred<QubeContainerProxy> = CompletableDeferred()) : ClientRequest()
        data class Destroy(val uuid: UUID) : ClientRequest()
    }

    private val pending = mutableMapOf<UUID, Create>()
    private val active = mutableMapOf<UUID, SendChannel<QubeException>>()
    private val fromClients = Channel<ClientRequest>()
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
                fromClients.onReceive {
                    when (it) {
                        is Create -> handleCreateRequest(it)
                        is Destroy -> handleDestroyRequest(it)
                    }
                }

                fromQube.onReceive {
                    when (it) {
                        is Ready -> handleReadyResponse(it)
                        is Error -> handleErrorResponse(it)
                    }
                }
            }
        }
    }

    private suspend fun handleCreateRequest(request: Create) {
        val uuid = nextUuid()
        LOG.info("[$uuid] -> CREATE")

        pending[uuid] = request
        toQube.send(QubeRequest.Create(uuid))
    }

    private suspend fun handleDestroyRequest(request: Destroy) {
        LOG.info("[${request.uuid}] -> DESTROY")

        pending.remove(request.uuid)
        active.remove(request.uuid)
        toQube.send(QubeRequest.Destroy(request.uuid))
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
                val channel = Channel<QubeException>(Channel.UNLIMITED)
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
