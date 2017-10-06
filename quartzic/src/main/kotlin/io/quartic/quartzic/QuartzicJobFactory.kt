package io.quartic.quartzic

import io.quartic.eval.api.EvalTriggerServiceClient
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle

class QuartzicJobFactory(
    val evalTriggerService: EvalTriggerServiceClient
): JobFactory {
    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
        when (bundle.jobDetail.jobClass) {
            EvalJob::class.java -> EvalJob(evalTriggerService)
            else -> bundle.jobDetail.jobClass.newInstance()
        }
}
