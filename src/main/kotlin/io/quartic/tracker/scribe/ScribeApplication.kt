package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubOptions
import com.google.cloud.storage.StorageOptions
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.tracker.scribe.healthcheck.PubSubSubscriptionHealthCheck
import java.time.Clock
import java.util.concurrent.TimeUnit

class ScribeApplication : ApplicationBase<ScribeConfiguration>() {
    // TODO: healthcheck for subscription
    // TODO: healthcheck for bucket
    // TODO: endpoint to trigger extract on-demand

    override fun runApplication(configuration: ScribeConfiguration, environment: Environment) {
        val pubsub = PubSubOptions.getDefaultInstance().service
        val storage = StorageOptions.getDefaultInstance().service

        val subscription = SubscriptionGetter(pubsub, configuration.pubsub.subscription!!).susbcription

        val writer = BatchWriter(
                storage,
                configuration.storage.bucket!!,
                configuration.storage.namespace!!
        )

        val extractor = MessageExtractor(
                subscription,
                Clock.systemUTC(),
                writer,
                configuration.batchSize!!
        )

        val ses = environment.lifecycle().scheduledExecutorService("executor").build()
        ses.scheduleAtFixedRate(
                extractor,
                configuration.extractionPeriodSeconds!! * 1000,
                configuration.extractionPeriodSeconds!! * 1000,
                TimeUnit.MILLISECONDS
        )

        environment.healthChecks().register("subscription", PubSubSubscriptionHealthCheck(pubsub, configuration.pubsub.subscription!!))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ScribeApplication().run(*args)
    }
}