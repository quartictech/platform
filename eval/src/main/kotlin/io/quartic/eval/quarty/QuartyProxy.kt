package io.quartic.eval.quarty

import com.google.common.base.Preconditions.checkState
import io.quartic.eval.EvaluatorException
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.quarty.model.QuartyRequest
import io.quartic.quarty.model.QuartyResponse
import io.quartic.quarty.model.QuartyResponse.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import java.time.Instant

private class QuartyProxy(
    hostname: String,
    quartyBuilder: (String) -> WebsocketClient<QuartyRequest, QuartyResponse>
) : AutoCloseable {
    private data class Context(
        val request: QuartyRequest,
        val log: suspend (String, String) -> Unit,
        val result: CompletableDeferred<Complete>
    )

    private val requests = Channel<Context>(Channel.UNLIMITED)
    private val quarty = quartyBuilder(hostname)
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
                { stream, message -> phase.log(stream, message, Instant.now()) },
                this
            ))
        }.await()

    private suspend fun runEventLoop() {
        var context: Context? = null

        while (true) {
            select<Unit> {
                quarty.events.onReceive {
                    when (it) {
                        is Connected -> {}        // TODO - we need to wait for this to happen
                        is Disconnected -> throw EvaluatorException("Connection to Quarty closed unexpectedly") // TODO - will this propagate to caller?
                        is MessageReceived -> {
                            checkState(context != null, "Message received unexpectedly: ${it}")

                            when (it.message) {
                                is Progress -> context!!.log("progress", it.message.message)
                                is Log -> context!!.log(it.message.stream, it.message.line)
                                is Complete -> {
                                    context!!.result.complete(it.message)
                                    context = null
                                }
                            }
                        }
                    }
                }

                requests.onReceive {
                    checkState(context == null, "Request received unexpectedly: ${it}")
                    quarty.outbound.send(it.request)
                    context = it
                }
            }
        }
    }
}
