package io.quartic.quartzic

import io.quartic.common.logging.logger
import io.quartic.qube.QubeProxy
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import org.quartz.Job
import org.quartz.JobExecutionContext

class QubeJob(val qubeProxy: QubeProxy) : Job {
    val LOG by logger()

    override fun execute(context: JobExecutionContext) {
        val pod = context.mergedJobDataMap.get("pod") as PodSpec

        launch(CommonPool) {
            val containerProxy = qubeProxy.createContainer(pod)
            LOG.info("[${containerProxy.id}] Container created")
        }
    }
}
