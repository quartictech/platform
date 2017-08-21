package io.quartic.qube.pods

import com.fasterxml.jackson.module.kotlin.readValue
import io.fabric8.kubernetes.api.model.Pod
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.Request
import io.quartic.qube.api.Response
import io.quartic.qube.store.JobStore
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*

class Qubicle(
    client: KubernetesClient,
    podTemplate: Pod,
    namespace: String,
    concurrentJobs: Int,
    jobStore: JobStore
) : AbstractVerticle() {
    private val LOG by logger()
    private val events = Channel<QubeEvent>()

    private val worker = WorkerImpl(client, podTemplate, namespace, jobStore)
    private val orchestrator = Orchestrator(events, worker, concurrentJobs)

    fun setupWebsocket(websocket: ServerWebSocket) {
        val clientUUID = UUID.randomUUID()
        val returnChannel = Channel<Response>()

        events.offer(QubeEvent.CreateClient(clientUUID))
        websocket.textMessageHandler { textMessage ->
            try {
                val message = OBJECT_MAPPER.readValue<Request>(textMessage)
                when (message) {
                    is Request.CreatePod -> events.offer(
                        QubeEvent.CreatePod(
                            PodKey(clientUUID, message.name),
                            returnChannel,
                            message.image,
                            message.command
                        )
                    )
                    is Request.DestroyPod -> events.offer(
                        QubeEvent.CancelPod(
                            PodKey(clientUUID, message.name)
                        )
                    )
                }
            }
            catch (e: Exception) {
                LOG.error("Closing websocket due to exception", e)
                websocket.close()
            }
        }

        websocket.closeHandler { events.offer(QubeEvent.CancelScope(clientUUID)) }

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
