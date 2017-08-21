package io.quartic.qube.pods

import com.fasterxml.jackson.module.kotlin.readValue
import io.fabric8.kubernetes.api.model.Pod
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.ReceivedMessage
import io.quartic.qube.api.SentMessage
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*

class Qubicle(client: KubernetesClient, podTemplate: Pod) : AbstractVerticle() {
    private val LOG by logger()
    private val events = Channel<QubeEvent>()

    private val worker = WorkerImpl(client, podTemplate)
    private val orchestrator = Orchestrator(events, worker)

    fun setupWebsocket(websocket: ServerWebSocket) {
        val scopeUUID = UUID.randomUUID()
        val returnChannel = Channel<SentMessage>()

        events.offer(QubeEvent.CreateScope(scopeUUID))
        websocket.textMessageHandler { textMessage ->
            try {
                val message = OBJECT_MAPPER.readValue<ReceivedMessage>(textMessage)
                when (message) {
                    is ReceivedMessage.CreatePod -> events.offer(
                        QubeEvent.CreatePod(
                            PodKey(scopeUUID, message.name),
                            returnChannel,
                            message.image,
                            message.command
                        )
                    )
                    is ReceivedMessage.RemovePod -> events.offer(
                        QubeEvent.CancelPod(
                            PodKey(scopeUUID, message.name)
                        )
                    )
                }
            }
            catch (e: Exception) {
                LOG.error("Closing websocket due to exception", e)
                websocket.close()
            }
        }

        websocket.closeHandler { events.offer(QubeEvent.CancelScope(scopeUUID)) }

        launch(CommonPool) {
            for (message in returnChannel) {
                websocket.writeTextMessage(OBJECT_MAPPER.writeValueAsString(message))
            }
        }
    }

    override fun start() {
        launch(CommonPool) {
            orchestrator.run()
        }

        vertx.createHttpServer()
            .websocketHandler { websocket ->
                LOG.info("Websocket connection")
                if (websocket.path().equals("/ws")) {
                    setupWebsocket(websocket)
                }
            }
            .listen(PORT) { res ->
               if (res.failed()) {
                   LOG.error("Could not start server", res.cause())
                   vertx.close()
               }
            }
    }

    companion object {
        val PORT = 8202
    }
}
