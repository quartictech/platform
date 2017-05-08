package io.quartic.mgmt

import io.dropwizard.Configuration
import io.quartic.catalogue.api.model.DatasetNamespace

class MgmtConfiguration : Configuration() {
    var catalogueUrl: String? = null
    var bucketName: String? = null
    var howlUrl: String? = null
    var defaultCatalogueNamespace: DatasetNamespace? = null
}
