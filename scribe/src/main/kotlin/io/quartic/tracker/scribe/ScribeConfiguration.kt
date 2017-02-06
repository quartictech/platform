package io.quartic.tracker.scribe

import io.dropwizard.Configuration

class ScribeConfiguration : Configuration() {
    class PubSubConfiguration {
        var subscription: String? = null
    }

    class StorageConfiguration {
        var bucket: String? = null
        var namespace: String? = null
    }

    val pubsub = PubSubConfiguration()
    val storage = StorageConfiguration()
    var batchSize: Int? = null
    var extractionPeriodSeconds: Long? = null
}


