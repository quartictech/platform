package io.quartic.quartzic

import io.quartic.common.logging.logger
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.BuildTrigger
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.Instant

class EvalJob(val triggerService: EvalTriggerServiceClient) : Job {
    private val LOG by logger()

    override fun execute(context: JobExecutionContext) {
        val triggerConfig = context.mergedJobDataMap["trigger"] as JobConfiguration.EvalTrigger

        val trigger = BuildTrigger.Manual(
            user = "quartzic",
            timestamp = Instant.now(),
            customerId = triggerConfig.customerId,
            branch = triggerConfig.branch,
            triggerType = triggerConfig.triggerType
        )

        val uuid = triggerService.triggerAsync(trigger).get()
        LOG.info("Launched build: ${uuid}")
    }
}
