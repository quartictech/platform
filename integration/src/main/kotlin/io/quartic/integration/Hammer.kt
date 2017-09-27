package io.quartic.integration

import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.BuildTrigger
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.*

object Hammer {
    val uri = URI.create("http://localhost:8210/api")
    val LOG by logger()

    @JvmStatic
    fun main(args: Array<String>) {
        val clientBuilder = ClientBuilder(Hammer::class.java)
        val eval = clientBuilder.retrofit<EvalTriggerServiceClient>(uri)
        val evalQuery = clientBuilder.retrofit<EvalQueryServiceClient>(uri)

        val run = 10
        val builds = mutableSetOf<UUID>()

        0.until(run).forEach {
            try {
                val buildId = eval.triggerAsync(BuildTrigger.Manual(
                    "mchammer",
                    Instant.now(),
                    CustomerId(115),
                    "develop",
                    BuildTrigger.TriggerType.EVALUATE
                )).get()
                LOG.info("[$buildId] Created build")
                builds.add(buildId)
            }
            catch (e: Exception) {
                LOG.error("Exception while creating build")
            }

            builds.forEach { buildId ->
            }

            Thread.sleep(5000)
        }
    }
}
