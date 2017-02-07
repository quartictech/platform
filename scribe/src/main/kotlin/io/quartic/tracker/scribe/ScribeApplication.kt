package io.quartic.tracker.scribe

import com.codahale.metrics.MetricRegistry
import com.google.cloud.pubsub.PubSub
import com.google.cloud.pubsub.PubSubOptions
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.dropwizard.setup.Environment
import io.quartic.catalogue.CatalogueWatcher
import io.quartic.common.application.ApplicationBase
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.tracker.scribe.healthcheck.PubSubSubscriptionHealthCheck
import io.quartic.tracker.scribe.healthcheck.StorageBucketHealthCheck
import java.time.Clock
import java.util.concurrent.TimeUnit

class ScribeApplication : ApplicationBase<ScribeConfiguration>() {
    // TODO: endpoint to trigger extract on-demand

    override fun runApplication(configuration: ScribeConfiguration, environment: Environment) {
        val pubsub = PubSubOptions.getDefaultInstance().service
        val storage = StorageOptions.getDefaultInstance().service

        with(environment.healthChecks()) {
            register("subscription", PubSubSubscriptionHealthCheck(pubsub, configuration.pubsub.subscription!!))
            register("bucket", StorageBucketHealthCheck(storage, configuration.storage.bucket!!))
        }

        createCatalogue(configuration).events.subscribe(::println)

        val ses = environment.lifecycle().scheduledExecutorService("executor").build()
        ses.scheduleAtFixedRate(
                createPipelineRunnable(configuration, pubsub, storage, environment.metrics()),
                configuration.extractionPeriodSeconds!! * 1000,
                configuration.extractionPeriodSeconds!! * 1000,
                TimeUnit.MILLISECONDS
        )
    }

    private fun createCatalogue(configuration: ScribeConfiguration) = CatalogueWatcher(
            WebsocketListener.Factory(configuration.catalogue.watchUrl, WebsocketClientSessionFactory(javaClass))
    )

    private fun createPipelineRunnable(
            configuration: ScribeConfiguration,
            pubsub: PubSub,
            storage: Storage,
            metrics: MetricRegistry
    ) = MessageExtractor(
            pubsub,
            configuration.pubsub.subscription!!,
            Clock.systemUTC(),
            createBatchWriter(configuration, storage, metrics),
            configuration.batchSize!!,
            metrics
    )

    private fun createBatchWriter(
            configuration: ScribeConfiguration,
            storage: Storage,
            metrics: MetricRegistry
    ) = BatchWriter(
            storage,
            configuration.storage.bucket!!,
            configuration.storage.namespace!!,
            metrics
    )

    companion object {
        @JvmStatic fun main(args: Array<String>) = ScribeApplication().run(*args)
    }
}