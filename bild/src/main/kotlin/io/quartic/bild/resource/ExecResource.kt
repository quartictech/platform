package io.quartic.bild.resource

import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildRequest
import io.quartic.bild.qube.JobPool
import io.quartic.common.uid.randomGenerator
import java.util.concurrent.ArrayBlockingQueue
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/exec")
class ExecResource(template: Job,
                   client: DefaultKubernetesClient = DefaultKubernetesClient()) {
    val idGenerator = randomGenerator { uid -> BildId(uid) }
    val queue = ArrayBlockingQueue<BildJob>(1024)

    init {
        JobPool(template, client, queue)
    }

    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    fun exec(@PathParam("customerId") customerId: String): BildId {
        val id = idGenerator.get()
        val bild = BildRequest(customerId)
        queue.put(BildJob(id, bild))
        return id
    }
}
