package io.quartic.qube.qube

import io.quartic.qube.model.JobResult
import io.quartic.common.logging.logger

class JobLoop {
    private val log by logger()

    fun loop(jobName: String, jobStateManager: JobStateManager): JobResult {
        while (true) {
            val jobState = jobStateManager.poll()
            when(jobState) {
                is JobStateManager.JobState.Running -> log.info("[{}] Running", jobName)
                is JobStateManager.JobState.Pending -> log.info("[{}] Pending", jobName)
                is JobStateManager.JobState.Failed -> return jobState.jobResult
                is JobStateManager.JobState.Succeeded -> return jobState.jobResult
            }

            log.info("[{}] Sleeping...", jobName)
            Thread.sleep(1000)
        }
    }
}
