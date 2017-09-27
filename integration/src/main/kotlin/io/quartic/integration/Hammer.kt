package io.quartic.integration

import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.BuildTrigger
import java.net.URI
import java.time.Instant
import java.util.*

object Hammer {
    val uri = URI.create("http://localhost:8210/api")
    val LOG by logger()
    val clientBuilder = ClientBuilder(Hammer::class.java)
    val eval = clientBuilder.retrofit<EvalTriggerServiceClient>(uri)
    val evalQuery = clientBuilder.retrofit<EvalQueryServiceClient>(uri)

    fun launchBatch(count: Int): MutableSet<UUID> {
        val builds = mutableSetOf<UUID>()

        0.until(count).forEach {
            try {
                val buildId = eval.triggerAsync(BuildTrigger.Manual(
                    "mchammer",
                    Instant.now(),

                    CustomerId(111),
                    "develop",
                    BuildTrigger.TriggerType.EVALUATE
                )).get()
                LOG.info("[$buildId] Created build")
                builds.add(buildId)
            } catch (e: Exception) {
                LOG.error("Exception while creating build")
            }
        }

        return builds
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val builds = launchBatch(10)

        while (builds.isNotEmpty()) {
            builds.retainAll { buildId ->
                val build = evalQuery.getBuildByIdAsync(buildId).get()


                if (build.status == "running") {
                    true
                } else {
                    build.events.filter { event -> event is ApiBuildEvent.}
                }
            }

            LOG.info("Waiting for 5s")
            Thread.sleep(5000)
        }
    }
}
