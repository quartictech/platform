package io.quartic.qube.pods

import com.fasterxml.jackson.module.kotlin.readValue
import io.fabric8.kubernetes.api.model.Pod
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.store.JobStore
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*

class Qubicle(
    private val websocketPort: Int,
    client: KubernetesClient,
    podTemplate: Pod,
    namespace: String,
    concurrentJobs: Int,
    jobTimeoutSeconds: Long,
    jobStore: JobStore
) : AbstractVerticle() {
    private val LOG by logger()
    private val events = Channel<QubeEvent>(UNLIMITED)

    private val worker = WorkerImpl(client, podTemplate, namespace, jobStore, jobTimeoutSeconds)
    private val orchestrator = Orchestrator(events, worker, concurrentJobs)

    private fun setupWebsocket(websocket: ServerWebSocket) {
        val clientUUID = UUID.randomUUID()
        val returnChannel = Channel<QubeResponse>()

        websocket.closeHandler { events.offer(QubeEvent.CancelClient(clientUUID)) }
        events.offer(QubeEvent.CreateClient(clientUUID))
        websocket.textMessageHandler { textMessage ->
            try {
                val message = OBJECT_MAPPER.readValue<QubeRequest>(textMessage)
                when (message) {
                    is QubeRequest.Create -> events.offer(
                        QubeEvent.CreatePod(
                            PodKey(clientUUID, message.name),
                            returnChannel,
                            message.container
                        )
                    )
                    is QubeRequest.Destroy -> events.offer(
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
                LOG.info("[${websocket.remoteAddress()}] Websocket connection")
                setupWebsocket(websocket)
            }
            .listen(websocketPort) { res ->
               if (res.failed()) {
                   LOG.error("Could not start server", res.cause())
                   vertx.close()
                   System.exit(1)
               }
            }
    }
}
