package io.quartic.quartzic

import org.quartz.Job
import org.quartz.JobExecutionContext

class QubeJob : Job {
    override fun execute(context: JobExecutionContext?) {
        println("Well Hello")
    }
}
