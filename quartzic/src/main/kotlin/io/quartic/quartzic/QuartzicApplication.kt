package io.quartic.quartzic

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.websocket.WebsocketClientImpl
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

class QuartzicApplication : ApplicationBase<QuartzicConfiguration>() {
    override fun runApplication(configuration: QuartzicConfiguration, environment: Environment) {
        val scheduler = configureScheduler(configuration)
        scheduler.start()
    }

    private fun configureScheduler(configuration: QuartzicConfiguration): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()

        configuration.jobs.forEach { jobConfig ->
            val jobDetail = JobBuilder.newJob(QubeJob::class.java)
                .withIdentity(jobConfig.name)
                .build()

            val trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cronSchedule))
                .build()

            scheduler.scheduleJob(jobDetail, trigger)
        }

        return scheduler
    }

    private fun qube(config: QuartzicConfiguration) = QubeProxy.create(
        WebsocketClientImpl.create(config.qubeUrl)
    )

    companion object {
        @JvmStatic fun main(args: Array<String>) = QuartzicApplication().run(*args)
    }
}
