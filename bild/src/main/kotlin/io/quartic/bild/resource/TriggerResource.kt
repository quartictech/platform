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
import io.quartic.registry.api.RegistryServiceAsync
import java.util.concurrent.BlockingQueue

class TriggerResource(
    private val queue: BlockingQueue<BildJob>,
    private val registry: RegistryServiceAsync,
    private val idGenerator: UidGenerator<BildId> = randomGenerator { uid -> BildId(uid) }
) : BildTriggerService {
    private val LOG by logger()

    override fun trigger(trigger: TriggerDetails) {
        LOG.info("Received trigger: ${trigger}")
        registry.getCustomerAsync(null, trigger.repoId)
            .thenAccept{ customer ->
                val id = idGenerator.get()
                LOG.info("Initiating build for customer '{}'. Queue has size {}", customer.id, queue.size)
                queue.put(BildJob(id, CustomerId(customer.id), trigger.installationId, trigger.cloneUrl, trigger.ref, trigger.commit, BildPhase.TEST))
            }
            .exceptionally { e ->
                LOG.error("Exception while contacting registry", e)
                null
            }
    }
}
