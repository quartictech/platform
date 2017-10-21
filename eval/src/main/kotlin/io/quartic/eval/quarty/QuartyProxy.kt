package io.quartic.eval.quarty

import io.quartic.common.auth.internal.InternalTokenGenerator
import io.quartic.common.auth.internal.InternalUser
import io.quartic.eval.EvaluatorException
import io.quartic.eval.quarty.QuartyProxy.State.*
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.quarty.api.model.QuartyAuthenticatedRequest
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyResponse
import io.quartic.quarty.api.model.QuartyResponse.*
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import java.net.URI

class QuartyProxy(
    private val customer: Customer,
    private val tokenGenerator: InternalTokenGenerator,
    private val quarty: WebsocketClient<QuartyAuthenticatedRequest, QuartyResponse>
) : AutoCloseable {
    constructor(customer: Customer, tokenGenerator: InternalTokenGenerator, hostname: String) : this(
        customer,
        tokenGenerator,
        WebsocketClientImpl.create(
            URI("http://${hostname}:${QUARTY_PORT}"),
            WebsocketClientImpl.ABORT_ON_FAILURE
        )
    )

    private data class Context(
        val request: QuartyRequest,
        val log: (String, String) -> Unit,
        val result: CompletableDeferred<Complete>
    )

    private sealed class State {
        class Unopened : State()
        class AwaitingRequest : State()
        class ServicingRequest(val context: Context) : State()
        class Failed : State()
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

    suspend fun request(
        request: QuartyRequest,
        log: (String, String) -> Unit
    ) = CompletableDeferred<Complete>()
        .apply { requests.send(Context(request, log, this)) }
        .await()

    private suspend fun runEventLoop() {
        while (true) {
            val state = state
            when (state) {
                is Unopened -> {
                    val event = quarty.events.receive()
                    when (event) {
                        is Connected -> this.state = AwaitingRequest()
                        else -> this.state = Failed()
                    }
                }

                is AwaitingRequest -> {
                    val context = requests.receive()
                    quarty.outbound.send(QuartyAuthenticatedRequest(generateToken(), context.request))
                    this.state = ServicingRequest(context)
                }

                is ServicingRequest -> {
                    val event = quarty.events.receive()
                    when (event) {
                        is Connected -> {}  // This should never happen
                        is Disconnected -> {
                            handleDisconnected(state.context)
                            this.state = Failed()
                        }
                        is MessageReceived -> handleMessage(state.context, event.message)
                    }
                }

                is Failed -> {
                    val context = requests.receive()
                    handleDisconnected(context) // Fail immediately
                }
            }
        }
    }

    // TODO - this is obviously fairly strange.  Longer term, the namespace list is probably more than just the "self" namespace.
    private fun generateToken() = tokenGenerator.generate(InternalUser(customer.namespace, listOf(customer.namespace)))

    private suspend fun handleDisconnected(context: Context) {
        context.result.completeExceptionally(EvaluatorException("Connection to Quarty unavailable"))
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
