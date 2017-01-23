package io.quartic.tracker.scribe

import io.dropwizard.Configuration

class ScribeConfiguration : Configuration() {
    class PubSubConfiguration {
        var subscription: String? = null
    }

    val pubsub = PubSubConfiguration()
}


