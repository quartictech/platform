package io.quartic.bild.resource

import io.quartic.bild.api.BildTriggerService
import io.quartic.bild.api.model.TriggerDetails
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.store.JobStore
import io.quartic.common.logging.logger
import io.quartic.registry.api.RegistryServiceClient
import java.util.concurrent.BlockingQueue

class TriggerResource(
    private val queue: BlockingQueue<BuildJob>,
    private val registry: RegistryServiceClient,
    private val jobStore: JobStore
) : BildTriggerService {
    private val LOG by logger()

    override fun trigger(trigger: TriggerDetails) {
        LOG.info("Received trigger: ${trigger}")
        registry.getCustomer(null, trigger.repoId)
            .thenAccept{ customer ->
                if (customer != null) {
                    val id = jobStore.createJob(customer.id, trigger.installationId, trigger.cloneUrl,
                        trigger.ref, trigger.commit, BuildPhase.TEST)
                    LOG.info("Initiating build for customer '{}'. Queue has size {}", customer.id, queue.size)
                    queue.put(BuildJob(id, customer.id, trigger.installationId, trigger.cloneUrl, trigger.ref, trigger.commit, BuildPhase.TEST))
                } else {
                    LOG.warn("Customer not found for repo: {}", trigger.repoId)
                }
            }
            .exceptionally { e ->
                LOG.error("Exception while contacting registry", e)
                null
            }
    }
}
