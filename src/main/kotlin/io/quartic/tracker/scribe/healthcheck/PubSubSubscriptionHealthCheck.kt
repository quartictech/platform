package io.quartic.tracker.scribe.healthcheck

import com.codahale.metrics.health.HealthCheck
import com.google.cloud.pubsub.PubSub

class PubSubSubscriptionHealthCheck(
        private val pubsub: PubSub,
        private val subscriptionName: String
) : HealthCheck() {
    override fun check(): Result = if (pubsub.getSubscription(subscriptionName) != null) {
        Result.healthy()
    } else {
        Result.unhealthy("Subscription '$subscriptionName' does not exist")
    }
}