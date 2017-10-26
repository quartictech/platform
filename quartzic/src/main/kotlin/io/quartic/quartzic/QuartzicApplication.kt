package io.quartic.quartzic

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.api.EvalTriggerServiceClient
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

class QuartzicApplication : ApplicationBase<QuartzicConfiguration>() {
    override fun runApplication(configuration: QuartzicConfiguration, environment: Environment) {
        val eval = eval(configuration)
        val scheduler = configureScheduler(configuration, eval)
        scheduler.start()
    }

    private fun eval(configuration: QuartzicConfiguration): EvalTriggerServiceClient =
        clientBuilder.retrofit(
            configuration.evalUrl
        )

    private fun configureScheduler(
        configuration: QuartzicConfiguration,
        evalTriggerService: EvalTriggerServiceClient
    ): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.setJobFactory(QuartzicJobFactory(evalTriggerService))

        configuration.jobs.forEach { jobConfig ->

            val jobDetail = when (jobConfig) {
                is JobConfiguration.EvalJobConfiguration -> {
                    val jobData = JobDataMap(mapOf("trigger" to jobConfig.trigger))
                    JobBuilder.newJob(EvalJob::class.java)
                        .withIdentity(jobConfig.name)
                        .setJobData(jobData)
                        .build()
                }
            }

            val trigger = TriggerBuilder.newTrigger()
//                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForTotalCount(1))
                .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cronSchedule))
                .build()

            scheduler.scheduleJob(jobDetail, trigger)
        }

        return scheduler
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = QuartzicApplication().run(*args)
    }
}
