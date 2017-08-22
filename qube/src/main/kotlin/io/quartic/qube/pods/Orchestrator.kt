package io.quartic.qube.pods

import io.quartic.common.logging.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.selects.select

class Orchestrator(
    private val events: Channel<QubeEvent>,
    private val worker: Worker,
    private val concurrency: Int,
    private val state: OrchestratorState = OrchestratorState()
) {
    private val LOG by logger()

    suspend fun run() {
        try {
            while (true) {
                val message = select<QubeEvent> {
                    events.onReceive { it }
                    state.runningPods.forEach { key, job ->
                        job.onJoin { QubeEvent.PodTerminated(key) }
                    }
                }

                LOG.info("Message received {}", message)
                when (message) {
                    is QubeEvent.CreatePod -> state.createPod(message)
                    is QubeEvent.CreateClient -> state.createClient(message)
                    is QubeEvent.CancelPod -> state.cancelRunningPod(message.key)
                    is QubeEvent.CancelClient -> state.cancelClient(message.client)
                    is QubeEvent.PodTerminated -> state.removeRunningPod(message.key)
                }

                // Drain waiting list
                state.drainWaitingList(concurrency) { create ->
                    async(CommonPool) {
                        worker.run(create)
                    }
                }
            }
        }
        finally {
            state.cancelAll()
        }
    }
}
