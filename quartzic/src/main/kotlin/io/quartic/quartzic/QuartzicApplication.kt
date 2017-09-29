package io.quartic.quartzic

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.qube.QubeProxy
import io.quartic.qube.websocket.WebsocketClientImpl
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

class QuartzicApplication : ApplicationBase<QuartzicConfiguration>() {
    override fun runApplication(configuration: QuartzicConfiguration, environment: Environment) {
        val qube = qube(configuration)
        val scheduler = configureScheduler(configuration, qube)
        scheduler.start()
    }

    private fun configureScheduler(configuration: QuartzicConfiguration, qubeProxy: QubeProxy): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.setJobFactory(QubeJobFactory(qubeProxy))

        configuration.jobs.forEach { jobConfig ->
            val jobData = JobDataMap(mapOf("pod" to jobConfig.pod))
            val jobDetail = JobBuilder.newJob(QubeJob::class.java)
                .withIdentity(jobConfig.name)
                .setJobData(jobData)
                .build()

            val trigger = TriggerBuilder.newTrigger()
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForTotalCount(1))
//                .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cronSchedule))
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
