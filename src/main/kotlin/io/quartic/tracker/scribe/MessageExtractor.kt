package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubException
import com.google.cloud.pubsub.Subscription
import io.quartic.common.logging.logger

class MessageExtractor(
        private val subscription: Subscription,
        private val handler: (List<String>) -> Boolean,
        private val batchSize: Int
) : Runnable {
    private val LOG by logger()

    // TODO: there's probably some stuff needed around modifying ack deadlines
    // TODO: go reactive?  (How to deal with acks in that case?)

    override fun run() {
        try {
            do {
                val messages = subscription.pull(batchSize).asSequence().toList()

                if (messages.isEmpty()) {
                    LOG.info("Pulled no messages from subscription, skipping handling")
                    return
                }
                LOG.info("Pulled ${messages.size} messages from subscription")

                val success = handler(messages.map {
                    val ret = it.payloadAsString
                    LOG.info("Message size = ${ret.length} bytes")
                    ret
                })

                // This is all or nothing - either ack everything or nothing
                if (success) {
                    LOG.info("Handler succeeded - sending acks")
                    messages.forEach { it.ack() }
                } else {
                    LOG.error("Handler failed - not sending acks")  // This is a major emo situation
                }
            } while (success && (messages.size == batchSize))    // We keep going until < batchSize, because that implies we exhausted the topic
        } catch (e: PubSubException) {
            LOG.error("Error interacting with PubSub", e)
        }
    }
}