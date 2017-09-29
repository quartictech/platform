package io.quartic.quartzic

import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.qube.QubeProxy
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle

class QuartzicJobFactory(
    val qubeProxy: QubeProxy,
    val evalTriggerService: EvalTriggerServiceClient
): JobFactory {
    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
        when (bundle.jobDetail.jobClass) {
            QubeJob::class.java -> QubeJob(qubeProxy)
            EvalJob::class.java -> EvalJob(evalTriggerService)
            else -> bundle.jobDetail.jobClass.newInstance()
        }
}
