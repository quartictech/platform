package io.quartic.bild.resource

import io.quartic.bild.api.BildTriggerService
import io.quartic.bild.api.model.TriggerDetails
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import java.util.concurrent.BlockingQueue
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

class TriggerResource(
    private val queue: BlockingQueue<BildJob>,
    private val idGenerator: UidGenerator<BildId> = randomGenerator { uid -> BildId(uid) }
) : BildTriggerService {
    private val LOG by logger()

    override fun trigger(trigger: TriggerDetails) {
        // TODO: look up customerId or whatever from Registry
        LOG.info("Received trigger: ${trigger}")
    }

    @Path("/{customerId}/{phase}")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    fun exec(@PathParam("customerId") customerId: CustomerId, @PathParam("phase") phase: BildPhase): BildId {
        val id = idGenerator.get()
        LOG.info("Queue has size {}", queue.size)
        queue.put(BildJob(id, customerId, phase))
        return id
    }
}
