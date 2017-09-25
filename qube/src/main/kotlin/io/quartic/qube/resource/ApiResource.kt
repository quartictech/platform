package io.quartic.qube.resource

import io.quartic.qube.pods.OrchestratorStateQueryApi
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class ApiResource(private val orchestratorState: OrchestratorStateQueryApi) {
    @GET
    @Path("/waiting")
    fun waitingList() = orchestratorState.getWaitingList()

    @GET
    @Path("/running")
    fun runningPods() = orchestratorState.getRunningPods()

    @GET
    @Path("/clients")
    fun clients() = orchestratorState.getClients()
}
