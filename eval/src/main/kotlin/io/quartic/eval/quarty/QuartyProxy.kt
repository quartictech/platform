package io.quartic.eval.quarty

import com.google.common.base.Preconditions.checkState
import io.quartic.eval.EvaluatorException
import io.quartic.eval.quarty.QuartyProxy.State.*
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyResponse
import io.quartic.quarty.api.model.QuartyResponse.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import java.net.URI

class QuartyProxy(private val quarty: WebsocketClient<QuartyRequest, QuartyResponse>) : AutoCloseable {
    constructor(hostname: String) : this(
        WebsocketClientImpl.create(
            URI("http://${hostname}:${QUARTY_PORT}"),
            WebsocketClientImpl.NO_RECONNECTION
        )
    )

    private data class Context(
        val request: QuartyRequest,
        val log: suspend (String, String) -> Unit,
        val result: CompletableDeferred<Complete>
    )

    private sealed class State {
        class Unopened : State()
        class AwaitingRequest : State()
        class ServicingRequest(val context: Context) : State()
    }

    private var state: State = Unopened()
    private val requests = Channel<Context>(Channel.UNLIMITED)
    private val job = launch(CommonPool) {
        try {
            runEventLoop()
        } finally {
            quarty.close()
            requests.close()
        }
    }

    override fun close() {
        job.cancel()
    }

    // TODO - get rid of direct dependency on PhaseBuilder
    suspend fun request(phase: PhaseBuilder<*>, request: QuartyRequest) =
        CompletableDeferred<Complete>().apply {
            requests.send(Context(
                request,
                { stream, message -> phase.log(stream, message) },
                this
            ))
        }.await()

    private suspend fun runEventLoop() {
        while (true) {
            // TODO - we have no mechanism to detect failing to connect - need to modify WebsocketClientImpl
            val state = state
            when (state) {
                is Unopened -> {
                    val event = quarty.events.receive()
                    checkState(event is Connected, "First websocket event is not Connected")
                    this.state = AwaitingRequest()
                }

                is AwaitingRequest -> {
                    val context = requests.receive()
                    quarty.outbound.send(context.request)
                    this.state = ServicingRequest(context)
                }

                is ServicingRequest -> {
                    val event = quarty.events.receive()
                    when (event) {
                        is Connected -> {}  // This should never happen
                        is Disconnected -> {
                            handleDisconnected(state.context)
                            return
                        }
                        is MessageReceived -> { handleMessage(state.context, event.message) }
                    }
                }
            }
        }
    }

    private suspend fun handleDisconnected(context: Context) {
        context.result.completeExceptionally(EvaluatorException("Connection to Quarty closed unexpectedly"))
    }

    private suspend fun handleMessage(context: Context, message: QuartyResponse) {
        when (message) {
            is Progress -> context.log("progress", message.message)
            is Log -> context.log(message.stream, message.line)
            is Complete -> {
                context.result.complete(message)
                state = AwaitingRequest()
            }
        }
    }

    companion object {
        val QUARTY_PORT = 8080
    }
}
