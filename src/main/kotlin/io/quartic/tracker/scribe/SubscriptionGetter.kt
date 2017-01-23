package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSub
import com.google.cloud.pubsub.Subscription
import io.quartic.common.logging.logger
import io.quartic.tracker.scribe.SubscriptionGetter.Companion.POLL_PERIOD_MILLIS

// TODO: this could be way more elegant/robust - e.g. a delegation wrapper around a Subscription object
class SubscriptionGetter(
        private val pubsub: PubSub,
        private val subscriptionName: String,
        private val pollPeriodMillis: Long = POLL_PERIOD_MILLIS
) {
    private val LOG by logger()

    val susbcription: Subscription
        get() {
            while (true) {
                try {
                    val subscription = pubsub.getSubscription(subscriptionName)
                    if (subscription != null) {
                        return subscription
                    } else {
                        throw RuntimeException("Subscription '$subscriptionName' does not exist")
                    }
                } catch (e: Exception) {
                    LOG.warn("Unable to acquire subscription, will try again in $pollPeriodMillis milliseconds", e)
                }

                Thread.sleep(pollPeriodMillis)
            }
        }

    companion object {
        val POLL_PERIOD_MILLIS = 5000L
    }
}