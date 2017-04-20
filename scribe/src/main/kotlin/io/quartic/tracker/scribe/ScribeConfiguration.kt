package io.quartic.tracker.scribe

import io.dropwizard.Configuration
import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.model.DatasetNamespace

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
    val catalogue = CatalogueClientConfiguration()
    var defaultCatalogueNamespace: DatasetNamespace? = null
    var batchSize: Int? = null
    var extractionPeriodSeconds: Long? = null

}


