package io.quartic.tracker.scribe

import com.google.cloud.pubsub.Subscription

class Thinger(private val subscription: Subscription) : Runnable {
    override fun run() {
        for (msg in subscription.pull(10)) {
            println(msg.payloadAsString)
            msg.ack()
        }
    }
}