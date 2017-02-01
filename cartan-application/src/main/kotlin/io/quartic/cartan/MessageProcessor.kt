package io.quartic.cartan;

import com.google.cloud.pubsub.Message
import com.google.cloud.pubsub.PubSub

class MessageProcessor(val subscription: String, val pubsub: PubSub) {

    fun process() {
        pubsub.pullAsync(subscription, {msg -> processMessage(msg)}, arrayOf())
    }

    fun processMessage(message: Message) {

    }

}
