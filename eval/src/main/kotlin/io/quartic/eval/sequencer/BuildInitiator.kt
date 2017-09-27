package io.quartic.eval.sequencer

import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import retrofit2.HttpException
import java.util.*

class BuildInitiator(
    private val database: Database,
    private val registry: RegistryServiceClient,
    private val uuidGen: () -> UUID = { UUID.randomUUID() }
) {
    private val LOG by logger()
    private val threadPool = newFixedThreadPoolContext(2, "database")

    data class BuildContext(
        val trigger: BuildTrigger,
        val customer: Customer,
        val build: Database.BuildRow
    )

    suspend fun start(trigger: BuildTrigger): BuildContext? {
        val customer = getCustomer(trigger)

        if (customer != null) {
            return BuildContext(trigger, customer, insertBuild(customer.id, trigger))
        } else {
            return null
        }
    }

    private suspend fun insertBuild(customerId: CustomerId, trigger: BuildTrigger) = run(threadPool) {
        val buildId = uuidGen()
        database.insertBuild(buildId, customerId, trigger.branch())
        database.getBuild(buildId)
    }

    private suspend fun getCustomer(trigger: BuildTrigger) = cancellable(
        block = {
            when (trigger) {
                is GithubWebhook ->
                    registry.getCustomerAsync(null, trigger.repoId)
                is io.quartic.eval.api.model.BuildTrigger.Manual ->
                    registry.getCustomerByIdAsync(trigger.customerId)
            }.await()
        },
        onThrow = { t ->
            if (t is HttpException && t.code() == 404) {
                when (trigger) {
                    is GithubWebhook ->
                        LOG.warn("No customer found for webhook (repoId = ${trigger.repoId})")
                    is Manual ->
                        LOG.warn("No customer found for manual trigger (customerId = ${trigger.customerId})")
                }
            } else {
                LOG.error("Error while communicating with Registry", t)
            }
            null
        }
    )
}
