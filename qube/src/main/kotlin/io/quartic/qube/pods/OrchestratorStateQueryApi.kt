package io.quartic.qube.pods

import java.util.*

interface OrchestratorStateQueryApi {
    fun getWaitingList(): Queue<QubeEvent.CreatePod>
    fun getClients(): Set<UUID>
    fun getRunningPods(): Set<PodKey>
}
