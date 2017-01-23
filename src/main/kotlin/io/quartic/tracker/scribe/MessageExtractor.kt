package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubException
import com.google.cloud.pubsub.Subscription
import io.quartic.common.logging.logger

// TODO: go reactive?  (How to deal with acks in that case?)
class MessageExtractor(
        private val subscription: Subscription,
        private val handler: (List<String>) -> Boolean,
        private val batchSize: Int
) : Runnable {
    val LOG by logger()

    override fun run() {
        try {
            do {
                val messages = subscription.pull(batchSize).asSequence().toList()

                if (messages.isEmpty()) {
                    LOG.info("Pulled no messages from subscription, skipping handling")
                    return
                }
                LOG.info("Pulled ${messages.size} messages from subscription")

                val success = handler(messages.map { it.payloadAsString })

                // This is all or nothing - either ack everything or nothing
                if (success) {
                    LOG.info("Handler succeeded - sending acks")
                    messages.forEach { it.ack() }
                } else {
                    LOG.warn("Handler failed - not sending acks")
                }
            } while (success && (messages.size == batchSize))    // We keep going until < batchSize, because that implies we exhausted the topic
        } catch (e: PubSubException) {
            LOG.error("Error interacting with PubSub", e)
        }
    }
}