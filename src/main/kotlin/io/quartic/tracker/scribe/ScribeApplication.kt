package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubOptions
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase

class ScribeApplication : ApplicationBase<ScribeConfiguration>() {
    // TODO: healthcheck for subscription
    // TODO: healthcheck for bucket

    override fun runApplication(configuration: ScribeConfiguration, environment: Environment) {
        val pubsub = PubSubOptions.getDefaultInstance().service
        val subscription = SubscriptionGetter(pubsub, configuration.pubsub.subscription!!).susbcription

        val ses = environment.lifecycle().scheduledExecutorService("Yeah").build()
//        ses.scheduleAtFixedRate(MessageExtractor(subscription), 1000, 1000, TimeUnit.MILLISECONDS)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = ScribeApplication().run(*args)
    }
}