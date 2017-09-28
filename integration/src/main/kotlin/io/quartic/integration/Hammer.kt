package io.quartic.integration

import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.BuildTrigger
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.time.Instant
import java.util.*

object Hammer {
    val uri = URI.create("http://localhost:8210/api")
    val LOG by logger()
    val clientBuilder = ClientBuilder(Hammer::class.java)
    val eval = clientBuilder.retrofit<EvalTriggerServiceClient>(uri)
    val evalQuery = clientBuilder.retrofit<EvalQueryServiceClient>(uri)

    data class BuildState(
        val startTime: Instant? = null,
        val endTime: Instant? = null,
        val termination: ApiBuildEvent? = null
    )

    fun launchBatch(count: Int): MutableMap<UUID, BuildState> {
        val builds = mutableMapOf<UUID, BuildState>()

        0.until(count).forEach {
            try {
                val buildId = eval.triggerAsync(BuildTrigger.Manual(
                    "mchammer",
                    Instant.now(),

                    CustomerId(222),
                    "develop",
                    BuildTrigger.TriggerType.EVALUATE
                )).get()
                LOG.info("[$buildId] Created build")
                builds[buildId] = BuildState(startTime = Instant.now())
            } catch (e: Exception) {
                LOG.error("Exception while creating build")
            }
        }

        return builds
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val builds = launchBatch(10)

        while (true) {
            val buildsLeft = builds.filter { it.value.endTime == null }

            if (buildsLeft.isEmpty()) {
                break
            }

            buildsLeft.forEach { buildId, state ->
                val build = evalQuery.getBuildByIdAsync(buildId).get()
                LOG.info("[${buildId}] ${build.status}")

                if (build.status != "running") {
                    val completionEvents = build.events.filter { event ->
                        event is ApiBuildEvent.BuildFailed || event is ApiBuildEvent.BuildSucceeded
                    }

                    if (completionEvents.isNotEmpty()) {
                        builds.put(buildId, state.copy(endTime = Instant.now(), termination = completionEvents.first()))
                    }
                }
            }

            LOG.info("Waiting for 5s")
            Thread.sleep(5000)
        }

        FileOutputStream("output.csv").use { output ->
            val writer = output.writer()
            builds.forEach { buildId, state ->
                val startTime = state.startTime?.epochSecond
                val endTime = state.endTime?.epochSecond
                val termination = state?.termination?.javaClass?.simpleName
                writer.write("$buildId,$startTime,$endTime,$termination\n")
            }
        }

    }
}
