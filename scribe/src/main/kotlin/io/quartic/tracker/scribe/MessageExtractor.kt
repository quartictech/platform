package io.quartic.tracker.scribe

import com.codahale.metrics.MetricRegistry
import com.google.cloud.pubsub.PubSub
import com.google.cloud.pubsub.PubSubException
import io.quartic.common.logging.logger
import io.quartic.common.metrics.meter
import io.quartic.common.metrics.timer
import java.time.Clock

class MessageExtractor(
        private val pubsub: PubSub,
        private val subscriptionName: String,
        private val clock: Clock,
        private val writer: BatchWriter,
        private val batchSize: Int,
        metrics: MetricRegistry
) : Runnable {
    private val LOG by logger()
    private val messagesMeter = meter(metrics, "messages")
    private val pubsubErrorMeter = meter(metrics, "pubsubError")
    private val ackLatency = timer(metrics, "ackLatency")

    // TODO: there's probably some stuff needed around modifying ack deadlines
    // TODO: go reactive?  (How to deal with acks in that case?)

    override fun run() {
        try {
            LOG.info("Extracting messages")
            val timestamp = clock.instant()
            var partNumber = 0
            fun String.nicely() = "[Part #$partNumber] $this"

            do {
                val messages = pubsub.pull(subscriptionName, batchSize).asSequence().toList()
                val doIt: () -> Boolean = {
                    if (messages.isEmpty()) {
                        LOG.info("Pulled no messages from subscription - skipping handling".nicely())
                        true
                    } else {
                        LOG.info("Pulled ${messages.size} messages from subscription".nicely())
                        messagesMeter.mark(messages.size.toLong())

                        writer.write(messages.map {
                            val ret = it.payloadAsString
                            LOG.info("Message size = ${ret.length} bytes".nicely())
                            ret
                        }, timestamp, partNumber)
                    }
                }
                val success = ackLatency.time(doIt)

                // This is all or nothing - either ack everything or nothing
                if (success) {
                    LOG.info("Handler succeeded - sending acks".nicely())
                    messages.forEach { it.ack() }
                } else {
                    LOG.error("Handler failed - not sending acks".nicely())  // This is a major emo situation
                }

                partNumber++
            } while (success && (messages.size == batchSize))    // We keep going until < batchSize, because that implies we exhausted the topic
        } catch (e: PubSubException) {
            LOG.error("Error interacting with PubSub", e)
            pubsubErrorMeter.mark()
        }
    }
}