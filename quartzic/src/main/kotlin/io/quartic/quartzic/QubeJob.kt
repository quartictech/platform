package io.quartic.quartzic

import io.quartic.common.logging.logger
import io.quartic.qube.QubeProxy
import io.quartic.qube.QubeProxy.QubeCompletion
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import java.util.concurrent.TimeUnit

class QubeJob(val qubeProxy: QubeProxy) : Job {
    val LOG by logger()

    override fun execute(context: JobExecutionContext) {
        val pod = context.mergedJobDataMap.get("pod") as PodSpec

        launch(CommonPool) {
            val containerProxy = qubeProxy.createContainer(pod)
            LOG.info("[${containerProxy.id}] Container created")

            withTimeout(1, TimeUnit.HOURS) {
                val completion = containerProxy.completion.receive()

                when (completion) {
                    is QubeCompletion.Terminated -> {
                        LOG.info("Job finished with message: ", completion.terminated.message)
                        if (completion.terminated !is Terminated.Succeeded) {
                            throw JobExecutionException("Job didn't terminate with success")
                        }
                    }
                    is QubeCompletion.Exception ->
                        throw JobExecutionException(completion.exception)
                }
            }
        }
    }
}
