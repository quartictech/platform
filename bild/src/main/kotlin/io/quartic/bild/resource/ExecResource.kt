package io.quartic.bild.resource

import io.quartic.bild.JobResultStore
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.model.CustomerId
import io.quartic.common.uid.randomGenerator
import java.util.concurrent.BlockingQueue
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/exec")
class ExecResource(val queue: BlockingQueue<BildJob>, val jobResults: JobResultStore) {
    val idGenerator = randomGenerator { uid -> BildId(uid) }

    @Path("/{customerId}/{phase}")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    fun exec(@PathParam("customerId") customerId: CustomerId, @PathParam("phase") phase: BildPhase): BildId {
        val id = idGenerator.get()
        queue.put(BildJob(id, customerId, phase))
        return id
    }

    @Path("/backchannel/{jobId}")
    @POST
    fun backchannel(@PathParam("jobId") jobId: BildId, data: Any) {
        jobResults.putExtraData(jobId, data)
    }
}
