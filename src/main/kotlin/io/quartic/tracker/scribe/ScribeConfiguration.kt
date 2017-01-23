package io.quartic.tracker

import io.dropwizard.Configuration

class ScribeConfiguration : Configuration() {
    class PubSubConfiguration {
        var emulated = false
        var topic: String? = null
    }

    val pubsub = PubSubConfiguration()
}


